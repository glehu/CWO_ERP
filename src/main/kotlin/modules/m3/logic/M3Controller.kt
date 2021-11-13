package modules.m3.logic

import api.logic.getCWOClient
import interfaces.IController
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.M2Contact
import modules.m2.logic.M2Controller
import modules.m3.M3Invoice
import modules.m3.gui.InvoiceConfiguratorWizard
import modules.m3.gui.MG3InvoiceFinder
import modules.m3.misc.InvoiceProperty
import modules.m3.misc.getInvoiceFromInvoiceProperty
import modules.m3.misc.getInvoicePropertyFromInvoice
import modules.m4.logic.M4PriceManager
import modules.mx.activeUser
import modules.mx.gui.userAlerts.MGXUserAlert
import modules.mx.logic.roundTo
import modules.mx.m3GlobalIndex
import tornadofx.Controller

@InternalAPI
@ExperimentalSerializationApi
class M3Controller : IController, Controller() {
    override val moduleNameLong = "M3Controller"
    override val module = "M3"
    override fun getIndexManager(): IIndexManager {
        return m3GlobalIndex!!
    }

    private val wizard = find<InvoiceConfiguratorWizard>()
    val client = getCWOClient(activeUser.username, activeUser.password)

    override fun searchEntry() {
        find<MG3InvoiceFinder>().openModal()
    }

    override fun newEntry() {
        wizard.invoice.commit()
        if (wizard.invoice.isValid && wizard.invoice.uID.value != -1) {
            setEntryLock(wizard.invoice.uID.value, false)
        }
        wizard.invoice.item = InvoiceProperty()
        wizard.invoice.validate()
        wizard.isComplete = false
    }

    override suspend fun saveEntry() {
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

    fun calculate(invoice: InvoiceProperty) {
        var pos = 0
        val categories = M4PriceManager().getCategories()
        val vat = (categories.priceCategories[invoice.priceCategory]?.vatPercent) ?: 0.0
        invoice.grossTotal = 0.0
        for (item in invoice.itemsProperty) {
            /**
             * Invoice specific calculation
             */
            invoice.grossTotal += (item.grossPrice * item.amount)

            /**
             * Line specific calculation
             */
            invoice.itemsProperty[pos].netPrice = (item.grossPrice / (1 + (vat / 100))).roundTo(2)
            pos++
        }
    }

    suspend fun processInvoice() {
        if (checkInvoice() && checkForProcess()) {
            var contact: M2Contact
            if (wizard.invoice.item.buyerUID != -1) {
                contact = M2Controller().get(wizard.invoice.item.buyerUID) as M2Contact
                contact.moneySent += wizard.invoice.item.paidGross
                M2Controller().save(contact)
            }
            if (wizard.invoice.item.sellerUID != -1) {
                contact = M2Controller().get(wizard.invoice.item.sellerUID) as M2Contact
                contact.moneyReceived += wizard.invoice.item.paidNet
                M2Controller().save(contact)
            }
            wizard.invoice.item.status = 4
            wizard.invoice.item.finished = true
            saveEntry()
        }
    }

    suspend fun setPaidInvoice() {
        if (checkInvoice() && checkForSetPaid()) {
            wizard.invoice.item.paidGross = wizard.invoice.item.grossTotal
            saveEntry()
        }
    }

    private fun checkForSetPaid(): Boolean {
        var valid = false
        if (wizard.invoice.item.paidGross < wizard.invoice.item.grossTotal) {
            valid = true
        } else {
            MGXUserAlert("Invoice is already paid.").openModal()
        }
        return valid
    }

    private fun checkForProcess(): Boolean {
        var valid = false
        if (wizard.invoice.item.paidGross == wizard.invoice.item.grossTotal) {
            valid = true
        } else if (wizard.invoice.item.paidGross < wizard.invoice.item.grossTotal) {
            MGXUserAlert("Paid amount is less than invoice total.").openModal()
        } else if (wizard.invoice.item.paidGross > wizard.invoice.item.grossTotal) {
            MGXUserAlert("Paid amount is more than invoice total.").openModal()
        }
        return valid
    }

    private fun checkInvoice(): Boolean {
        var valid = false
        wizard.invoice.validate()
        if (wizard.invoice.isValid) {
            wizard.invoice.commit()
            if (!wizard.invoice.item.finished) {
                valid = true
            } else {
                MGXUserAlert("The invoice is already finished.").openModal()
            }
        } else {
            MGXUserAlert(
                "Please fill out the invoice completely.\n\n" +
                        "Missing fields are marked red."
            ).openModal()
        }
        return valid
    }

    fun showToDoInvoices() {
        val m3Finder = MG3InvoiceFinder()
        m3Finder.exactSearch.isSelected = true
        m3Finder.ixNr.value = M3Controller().getIndexUserSelection()[3]
        m3Finder.openModal()
        m3Finder.searchText.text = ""
        m3Finder.searchText.text = "0"
    }
}
