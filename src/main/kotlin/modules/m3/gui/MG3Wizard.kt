package modules.m3.gui

import io.ktor.util.*
import javafx.collections.ObservableList
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.logic.M2Controller
import modules.m3.M3Item
import modules.m3.logic.M3Controller
import modules.m3.misc.InvoiceModel
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class InvoiceConfiguratorWizard : Wizard("Add new invoice") {
    val invoice: InvoiceModel by inject()

    init {
        enableStepLinks = true
        add(NewInvoiceMainData::class)
        add(NewInvoiceItemData::class)
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
        fieldset {
            field("UID") {
                textfield(invoice.uID).isEditable = false
            }
            field("Seller") {
                hbox {
                    textfield(invoice.seller) {
                        contextmenu {
                            item("Show contact").action {
                                if (invoice.sellerUID.value != -1) m2controller.showContact(invoice.sellerUID.value)
                                invoice.seller.value =
                                    m2controller.getContactName(invoice.sellerUID.value, invoice.seller.value)
                            }
                            item("Load contact").action {
                                val contact = m2controller.selectAndReturnContact()
                                invoice.sellerUID.value = contact.uID
                                invoice.seller.value = contact.name
                            }
                        }
                    }.required()
                    label(invoice.sellerUID) { paddingHorizontal = 20 }
                }
            }
            field("Buyer") {
                hbox {
                    textfield(invoice.buyer) {
                        contextmenu {
                            item("Load contact").action {
                                val contact = m2controller.selectAndReturnContact()
                                invoice.buyerUID.value = contact.uID
                                invoice.buyer.value = contact.name
                            }
                            item("Show contact").action {
                                if (invoice.buyerUID.value != -1) m2controller.showContact(invoice.buyerUID.value)
                                invoice.buyer.value =
                                    m2controller.getContactName(invoice.buyerUID.value, invoice.buyer.value)
                            }
                        }
                    }.required()
                    label(invoice.buyerUID) { paddingHorizontal = 20 }
                }
            }
            field("Date") { datepicker(invoice.date).required() }
            field("Text") { textfield(invoice.text).required() }
            field("Paid") {
                hbox {
                    textfield(invoice.paid) {
                        prefWidth = 100.0
                    }
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
    val items: ObservableList<M3Item> = observableListOf(M3Item(-1, ""))

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    override val root = form {
        items.clear()
        fieldset {
            field("Price") {
                hbox {
                    textfield(invoice.price) {
                        prefWidth = 100.0
                    }.required()
                    label("EUR") { paddingHorizontal = 20 }
                }
            }
            tableview(items) {
                readonlyColumn("Description", M3Item::description)
                readonlyColumn("Price", M3Item::price)
                readonlyColumn("Amount", M3Item::amount)
                readonlyColumn("User", M3Item::userName)
                this.columnResizePolicy = SmartResize.POLICY
            }
            button("Add Position") {
                action {
                    val item = m3Controller.createAndReturnItem()
                    item.initialize()
                    invoice.price += item.price
                    items.add(item)
                }
            }
        }
    }

    override fun onSave() {
        isComplete = invoice.commit()
    }
}