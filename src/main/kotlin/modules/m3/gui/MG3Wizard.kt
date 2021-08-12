package modules.m3.gui

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.Contact
import modules.m2.logic.M2Controller
import modules.m2.logic.M2DBManager
import modules.m3.misc.InvoiceModel
import modules.mx.m2GlobalIndex
import tornadofx.*

@ExperimentalSerializationApi
class InvoiceConfiguratorWizard : Wizard("Add new invoice")
{
    val invoice: InvoiceModel by inject()

    init
    {
        enableStepLinks = true
        add(NewInvoiceMainData::class)
    }
}

@ExperimentalSerializationApi
class InvoiceViewerWizard : Wizard("View an invoice")
{
    val invoice: InvoiceModel by inject()

    init
    {
        enableStepLinks = true
        add(NewInvoiceMainData::class)
    }
}

@ExperimentalSerializationApi
class NewInvoiceMainData : Fragment("Main")
{
    private val db: CwODB by inject()
    private val invoice: InvoiceModel by inject()
    private val m2controller: M2Controller by inject()

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    override val root = form {
        fieldset("Invoice") {
            field("Seller") {
                hbox {
                    textfield(invoice.seller) {
                        contextmenu {
                            item("Show contact").action {
                                if (invoice.sellerUID.value != -1) m2controller.showContact(
                                    M2DBManager().getEntry(
                                        invoice.sellerUID.value, db, m2GlobalIndex.indexList[0]!!
                                    ) as Contact
                                )
                                invoice.seller.value =
                                    m2controller.getContactName(
                                        invoice.sellerUID.value, invoice.seller.value
                                    )
                            }
                            item("Load contact").action {
                                val contact = m2controller.selectContact()
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
                                val contact = m2controller.selectContact()
                                invoice.buyerUID.value = contact.uID
                                invoice.buyer.value = contact.name
                            }
                            item("Show contact").action {
                                if (invoice.buyerUID.value != -1) m2controller.showContact(
                                    M2DBManager().getEntry(
                                        invoice.buyerUID.value, db, m2GlobalIndex.indexList[0]!!
                                    ) as Contact
                                )
                                invoice.buyer.value =
                                    m2controller.getContactName(
                                        invoice.buyerUID.value, invoice.buyer.value
                                    )
                            }
                        }
                    }.required()
                    label(invoice.buyerUID) { paddingHorizontal = 20 }
                }
            }
            field("Date") { datepicker(invoice.date).required() }
            field("Text") { textfield(invoice.text).required() }
            field("Price") {
                hbox {
                    textfield(invoice.price) {
                        prefWidth = 100.0
                    }.required()
                    label("EUR") { paddingHorizontal = 20 }
                }
            }
        }
    }

    override fun onSave()
    {
        isComplete = invoice.commit()
    }
}