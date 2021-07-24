package modules.m3.gui

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.Contact
import modules.m2.logic.M2Controller
import modules.m2.logic.M2DBManager
import modules.m2.logic.M2IndexManager
import modules.m3.misc.InvoiceModel
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
    val m2IndexManager: M2IndexManager by inject()

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
    private val m2IndexManager: M2IndexManager by inject()

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    override val root = form {
        fieldset("Invoice") {
            field("Seller") {
                hbox {
                    textfield(invoice.seller) {
                        contextmenu {
                            item("Show contact") {
                                if (invoice.sellerUID.value == -1) isDisable = true
                            }.action {
                                m2controller.showContact(
                                    M2DBManager().getEntry(
                                        invoice.sellerUID.value, db, m2IndexManager.indexList[0]!!
                                    ) as Contact, m2IndexManager, false
                                )
                                invoice.seller.value =
                                    m2controller.getContactName(
                                        invoice.sellerUID.value, invoice.seller.value, m2IndexManager
                                    )
                            }
                        }
                    }.required()
                    button("<") {
                        tooltip("Load an address")
                        action {
                            val contact = m2controller.selectContact(m2IndexManager)
                            invoice.sellerUID.value = contact.uID
                            invoice.seller.value = contact.name
                        }
                    }
                    label(invoice.sellerUID) { paddingHorizontal = 20 }
                }
            }
            field("Buyer") {
                hbox {
                    textfield(invoice.buyer) {
                        contextmenu {
                            item("Show contact") {
                                if (invoice.buyerUID.value == -1) isDisable = true
                            }.action {
                                m2controller.showContact(
                                    M2DBManager().getEntry(
                                        invoice.buyerUID.value, db, m2IndexManager.indexList[0]!!
                                    ) as Contact, m2IndexManager, false
                                )
                                invoice.seller.value =
                                    m2controller.getContactName(
                                        invoice.buyerUID.value, invoice.buyer.value, m2IndexManager
                                    )
                            }
                        }
                    }.required()
                    button("<") {
                        tooltip("Load an address")
                        action {
                            val contact = m2controller.selectContact(m2IndexManager)
                            invoice.buyerUID.value = contact.uID
                            invoice.buyer.value = contact.name
                        }
                    }
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