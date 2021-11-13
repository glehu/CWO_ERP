package modules.m3.gui

import io.ktor.util.*
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.m2.M2Contact
import modules.m2.logic.M2Controller
import modules.m3.M3InvoicePosition
import modules.m3.logic.M3Controller
import modules.m3.misc.InvoiceModel
import modules.m4.M4PriceCategory
import modules.m4.logic.M4Controller
import modules.mx.activeUser
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class InvoiceConfiguratorWizard : Wizard("Add new invoice") {
    val invoice: InvoiceModel by inject()

    init {
        enableStepLinks = true
        add(NewInvoiceMainData::class)
        add(NewInvoiceItemData::class)
        add(NewInvoiceNotes::class)
    }
}

@InternalAPI
@ExperimentalSerializationApi
class NewInvoiceMainData : Fragment("Main") {
    private val invoice: InvoiceModel by inject()
    private val m2controller: M2Controller by inject()

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    override val root = form {
        prefWidth = 400.0
        fieldset {
            field("UID") { label(invoice.uID) }
            field("Status") { label(invoice.status) }
            field("Finished") { label(invoice.finished) }
            field("Seller") {
                textfield(invoice.seller) {
                    contextmenu {
                        item("Load contact").action {
                            val contact = m2controller.selectAndReturnContact()
                            invoice.sellerUID.value = contact.uID
                            invoice.seller.value = contact.name
                        }
                        item("Show contact").action {
                            if (invoice.sellerUID.value != -1) m2controller.showEntry(invoice.sellerUID.value)
                            invoice.seller.value =
                                m2controller.getContactName(invoice.sellerUID.value, invoice.seller.value)
                        }
                    }
                }.required()
            }
            field("Buyer") {
                textfield(invoice.buyer) {
                    contextmenu {
                        item("Load contact").action {
                            val contact = m2controller.selectAndReturnContact()
                            invoice.buyerUID.value = contact.uID
                            invoice.buyer.value = contact.name
                            invoice.priceCategory.value = contact.priceCategory
                        }
                        item("Show contact").action {
                            if (invoice.buyerUID.value != -1) m2controller.showEntry(invoice.buyerUID.value)
                            invoice.buyer.value =
                                m2controller.getContactName(invoice.buyerUID.value, invoice.buyer.value)
                        }
                    }
                }.required()
            }
            field("Date") { datepicker(invoice.date).required() }
            field("Text") { textfield(invoice.text).required() }
            field("Paid") {
                hbox {
                    textfield(invoice.paidGross)
                    label("EUR") { paddingHorizontal = 20 }
                }
            }
        }
    }

    override fun onSave() {
        isComplete = invoice.commit()
    }
}

@InternalAPI
@ExperimentalSerializationApi
class NewInvoiceItemData : Fragment("Items") {
    private val invoice: InvoiceModel by inject()
    private val m3Controller: M3Controller by inject()
    private val table = tableview(invoice.items) {
        isEditable = true
        column("Description", M3InvoicePosition::description) {
            makeEditable()
            prefWidth = 300.0
        }
        column("Gross", M3InvoicePosition::grossPrice) {
            makeEditable()
            prefWidth = 100.0
        }
        readonlyColumn("Net", M3InvoicePosition::netPrice) {
            prefWidth = 100.0
        }
        column("Amount", M3InvoicePosition::amount) {
            makeEditable()
            prefWidth = 100.0
        }
        readonlyColumn("User", M3InvoicePosition::userName).prefWidth = 250.0

        onEditCommit {
            invoice.commit()
            m3Controller.calculate(invoice.item)
            this.tableView.refresh()
        }

        contextmenu {
            item("Remove") {
                action {
                    invoice.items.value.remove(selectedItem)
                }
                tooltip("Removes the selected item from the invoice")
            }
        }

        enableCellEditing()
        isFocusTraversable = false
    }

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    override val root = form {
        fieldset {
            field("Price Category") { label(invoice.priceCategory) }
            field("Total") {
                hbox {
                    textfield(invoice.grossTotal) {
                        prefWidth = 100.0
                        isEditable = false
                    }.required()
                    label("EUR") { paddingHorizontal = 20 }
                }
            }
            /**
             * Invoice positions table
             */
            add(table)
            hbox {
                button("Add Position") {
                    action {
                        val itemPosition = M3InvoicePosition(-1, "")
                        itemPosition.userName = activeUser.username
                        invoice.items.value.add(itemPosition)
                    }
                }
                button("Load Item") {
                    action {
                        val item = M4Controller().selectAndReturnItem()
                        val itemPosition = M3InvoicePosition(item.uID, item.description)
                        var priceCategory = 0
                        if (invoice.buyerUID.value != -1) {
                            val contact = M2Controller().get(invoice.buyerUID.value) as M2Contact
                            priceCategory = contact.priceCategory
                        }
                        itemPosition.grossPrice =
                            Json.decodeFromString<M4PriceCategory>(item.prices[priceCategory]!!).grossPrice
                        itemPosition.userName = activeUser.username
                        invoice.items.value.add(itemPosition)
                        invoice.commit()
                        m3Controller.calculate(invoice.item)
                    }
                }
                button("Remove item") {
                    action {
                        invoice.items.value.remove(table.selectedItem)
                        m3Controller.calculate(invoice.item)
                    }
                    tooltip("Removes the selected item from the invoice")
                    style { unsafe("-fx-base", Color.DARKRED) }
                }
            }
        }
    }

    override fun onSave() {
        isComplete = invoice.commit()
    }
}

@InternalAPI
@ExperimentalSerializationApi
class NewInvoiceNotes : Fragment("Notes") {
    private val invoice: InvoiceModel by inject()

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    override val root = form {
        prefWidth = 400.0
        fieldset {
            field("Customer Note") {
                textarea(invoice.customerNote) {
                    prefHeight = 100.0
                }
            }
            field("Internal Note") {
                textarea(invoice.internalNote) {
                    prefHeight = 100.0
                }
            }
        }
    }

    override fun onSave() {
        isComplete = invoice.commit()
    }
}
