package modules.m3.logic

import api.logic.getCWOClient
import interfaces.IController
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.M2Contact
import modules.m3.M3Invoice
import modules.m3.M3InvoicePosition
import modules.m3.gui.InvoiceConfiguratorWizard
import modules.m3.gui.ItemConfiguratorWizard
import modules.m3.gui.MG3InvoiceFinder
import modules.m3.misc.*
import modules.mx.activeUser
import modules.mx.gui.userAlerts.MGXUserAlert
import modules.mx.logic.MXLog
import modules.mx.m2GlobalIndex
import modules.mx.m3GlobalIndex
import tornadofx.Controller

@InternalAPI
@ExperimentalSerializationApi
class M3Controller : IController, Controller() {
    override val moduleNameLong = "M3Controller"
    override val module = "M3"
    override fun getIndexManager(): IIndexManager {
        return m3GlobalIndex
    }

    private val wizard = find<InvoiceConfiguratorWizard>()
    val client = getCWOClient(activeUser.username, activeUser.password)

    override fun searchEntry() {
        find<MG3InvoiceFinder>().openModal()
    }

    override fun newEntry() {
        wizard.invoice.item = InvoiceProperty()
        wizard.invoice.validate()
        wizard.isComplete = false
    }

    override fun saveEntry() {
        if (wizard.invoice.isValid) {
            wizard.invoice.commit()
            wizard.invoice.uID.value = save(getInvoiceFromInvoiceProperty(wizard.invoice.item))
            wizard.isComplete = false
        }
    }

    override fun showEntry(uID: Int) {
        val entry = get(uID) as M3Invoice
        wizard.invoice.item = getInvoicePropertyFromInvoice(entry)
    }

    fun createAndReturnItem(): M3InvoicePosition {
        val item = M3InvoicePosition(-1, "")
        val wizard = ItemConfiguratorWizard()
        wizard.showHeader = false
        wizard.showSteps = false
        wizard.item.item = getItemPropertyFromItem(item)
        wizard.openModal(block = true)
        return getItemFromItemProperty(wizard.item.item)
    }

    fun calculate(invoice: InvoiceProperty) {
        invoice.price = 0.0
        for (item in invoice.itemsProperty) {
            invoice.price += (item.price * item.amount)
        }
    }

    fun processInvoice() {
        wizard.invoice.commit()
        wizard.invoice.validate()
        if (wizard.invoice.isValid && !wizard.invoice.item.finished) {
            var contact: M2Contact
            if (wizard.invoice.item.buyerUID != -1) {
                contact = m2GlobalIndex.get(wizard.invoice.item.buyerUID) as M2Contact
                contact.moneySent += wizard.invoice.item.paid
                m2GlobalIndex.save(contact)
            }
            if (wizard.invoice.item.sellerUID != -1) {
                contact = m2GlobalIndex.get(wizard.invoice.item.sellerUID) as M2Contact
                contact.moneyReceived += wizard.invoice.item.paid
                m2GlobalIndex.save(contact)
            }
            wizard.invoice.item.finished = true
            saveEntry()
        } else if (wizard.invoice.item.finished) {
            MGXUserAlert(MXLog.LogType.INFO, "The invoice is already finished.").openModal()
        }
    }
}