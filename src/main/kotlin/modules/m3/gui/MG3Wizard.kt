package modules.m3.gui

import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.logic.M2Controller
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

    init
    {
        enableStepLinks = true
        add(NewInvoiceMainData::class)
    }
}

@ExperimentalSerializationApi
class NewInvoiceMainData : Fragment("Main")
{
    private val invoice: InvoiceModel by inject()
    private val m2controller: M2Controller by inject()

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    override val root = form {
        fieldset("Invoice") {
            field("Seller") {
                textfield(invoice.seller).required()
                label(invoice.sellerUID)
                button("<") {
                    tooltip("Load an address")
                    action {
                        val contact = m2controller.selectContact()
                        invoice.sellerUID.value = contact.uID
                        invoice.seller.value = contact.name
                    }
                }
            }
            field("Buyer") {
                textfield(invoice.buyer).required()
                label(invoice.buyerUID)
                button("<") {
                    tooltip("Load an address")
                    action {
                        val contact = m2controller.selectContact()
                        invoice.buyerUID.value = contact.uID
                        invoice.buyer.value = contact.name
                    }
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