package modules.m3.gui

import io.ktor.util.*
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.m2.Contact
import modules.m2.logic.ContactController
import modules.m3.InvoicePosition
import modules.m3.logic.InvoiceController
import modules.m3.misc.InvoiceModel
import modules.m4.ItemPriceCategory
import modules.m4.logic.ItemController
import modules.mx.activeUser
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class InvoiceConfiguratorWizard : Wizard("Add new invoice") {
    val invoice: InvoiceModel by inject()

    init {
        enableStepLinks = true
        add(InvoiceMainData::class)
        add(InvoiceItemData::class)
        add(InvoiceNotesData::class)
    }
}

@InternalAPI
@ExperimentalSerializationApi
class InvoiceMainData : Fragment("Main") {
    private val invoice: InvoiceModel by inject()
    private val contactController: ContactController by inject()

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    override val root = form {
        prefWidth = 400.0
        fieldset {
            field("UID") { label(invoice.uID) }
            field("Status") { label(invoice.status) }
            field("Status Text") { label(invoice.statusText) }
            field("Finished") {
                label(invoice.finished) {
                    tooltip("True, if the invoice got processed, applying transactions to the participants.")
                }
            }
            field("Notified") {
                label(invoice.emailConfirmationSent) {
                    tooltip("True, if the customer got notified by EMail.")
                }
            }
            field("Seller") {
                textfield(invoice.seller) {
                    contextmenu {
                        item("Load contact").action {
                            val contact = contactController.selectAndLoadContact()
                            invoice.sellerUID.value = contact.uID
                            invoice.seller.value = contact.name
                        }
                        item("Show contact").action {
                            if (invoice.sellerUID.value != -1) contactController.showEntry(invoice.sellerUID.value)
                            invoice.seller.value =
                                contactController.getContactName(invoice.sellerUID.value, invoice.seller.value)
                        }
                    }
                }.required()
            }
            field("Buyer") {
                textfield(invoice.buyer) {
                    contextmenu {
                        item("Load contact").action {
                            val contact = contactController.selectAndLoadContact()
                            invoice.buyerUID.value = contact.uID
                            invoice.buyer.value = contact.name
                            invoice.priceCategory.value = contact.priceCategory
                        }
                        item("Show contact").action {
                            if (invoice.buyerUID.value != -1) contactController.showEntry(invoice.buyerUID.value)
                            invoice.buyer.value =
                                contactController.getContactName(invoice.buyerUID.value, invoice.buyer.value)
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
class InvoiceItemData : Fragment("Items") {
    private val invoice: InvoiceModel by inject()
    private val invoiceController: InvoiceController by inject()
    private val table = tableview(invoice.items) {
        tooltip("Displays the invoices' positions.")
        isEditable = true
        column("Description", InvoicePosition::description) {
            makeEditable()
            prefWidth = 300.0
        }
        column("Gross", InvoicePosition::grossPrice) {
            makeEditable()
            prefWidth = 100.0
        }
        readonlyColumn("Net", InvoicePosition::netPrice) {
            prefWidth = 100.0
        }
        column("Amount", InvoicePosition::amount) {
            makeEditable()
            prefWidth = 100.0
        }
        readonlyColumn("User", InvoicePosition::userName).prefWidth = 250.0

        onEditCommit {
            invoice.commit()
            invoiceController.calculate(invoice.item)
            this.tableView.refresh()
        }

        contextmenu {
            item("Remove") {
                action {
                    invoice.items.value.remove(selectedItem)
                }
                tooltip("Removes the selected item from the invoice.")
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
                        val itemPosition = InvoicePosition(-1, "")
                        itemPosition.userName = activeUser.username
                        invoice.items.value.add(itemPosition)
                    }
                }
                button("Load Item") {
                    action {
                        val item = ItemController().selectAndReturnItem()
                        val itemPosition = InvoicePosition(item.uID, item.description)
                        var priceCategory = 0
                        if (invoice.buyerUID.value != -1) {
                            val contact = ContactController().get(invoice.buyerUID.value) as Contact
                            priceCategory = contact.priceCategory
                        }
                        itemPosition.grossPrice =
                            Json.decodeFromString<ItemPriceCategory>(item.prices[priceCategory]!!).grossPrice
                        itemPosition.userName = activeUser.username
                        invoice.items.value.add(itemPosition)
                        invoice.commit()
                        invoiceController.calculate(invoice.item)
                    }
                }
                button("Remove item") {
                    action {
                        invoice.items.value.remove(table.selectedItem)
                        invoiceController.calculate(invoice.item)
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
class InvoiceNotesData : Fragment("Notes") {
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
                    isEditable = false
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
