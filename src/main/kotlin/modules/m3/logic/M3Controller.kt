package modules.m3.logic

import interfaces.IController
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.M2Contact
import modules.m2.logic.M2Controller
import modules.m3.M3Invoice
import modules.m3.gui.InvoiceConfiguratorWizard
import modules.m3.gui.MG3InvoiceFinder
import modules.m3.gui.MG3InvoicePayer
import modules.m3.gui.MG3Settings
import modules.m3.misc.InvoiceProperty
import modules.m3.misc.getInvoiceFromInvoiceProperty
import modules.m3.misc.getInvoicePropertyFromInvoice
import modules.m4.M4StockPosting
import modules.m4.logic.M4PriceManager
import modules.mx.gui.userAlerts.MGXUserAlert
import modules.mx.logic.MXEMailer
import modules.mx.logic.MXLog
import modules.mx.logic.MXTimestamp
import modules.mx.logic.roundTo
import modules.mx.m3GlobalIndex
import modules.mx.m4StockPostingGlobalIndex
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

    override suspend fun saveEntry(unlock: Boolean) {
        if (wizard.invoice.isValid) {
            wizard.invoice.commit()
            wizard.invoice.uID.value = save(getInvoiceFromInvoiceProperty(wizard.invoice.item), unlock = unlock)
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
            //Stock Posting
            for (item in wizard.invoice.item.itemsProperty) {
                if (item.stockPostingUID != -1) {
                    val stockPosting =
                        m4StockPostingGlobalIndex!!.get(item.stockPostingUID) as M4StockPosting
                    stockPosting.dateBooked = MXTimestamp.now()
                    stockPosting.status = 9
                    stockPosting.isFinished = true
                    m4StockPostingGlobalIndex!!.save(stockPosting)
                }
            }
            wizard.invoice.item.status = 9
            wizard.invoice.item.statusText = M3CLIController().getStatusText(wizard.invoice.item.status)
            wizard.invoice.item.finished = true
            saveEntry()
        }
    }

    suspend fun payInvoice() {
        if (checkInvoice() && checkForSetPaid()) {
            val paymentScreen = find<MG3InvoicePayer>()
            paymentScreen.openModal(block = true)
            if (paymentScreen.userConfirmed) {
                wizard.invoice.item.paidGross = wizard.invoice.item.grossTotal
                wizard.invoice.item.paidNet = wizard.invoice.item.netTotal
                if (wizard.invoice.item.paidGross == wizard.invoice.item.grossTotal) {
                    wizard.invoice.item.status = 3
                } else {
                    wizard.invoice.item.status = 2
                }
                wizard.invoice.item.statusText = M3CLIController().getStatusText(wizard.invoice.item.status)
                saveEntry()
            }
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
        val iniVal = M3CLIController().getIni()
        val todoStatuses = iniVal.todoStatuses
        val m3Finder = MG3InvoiceFinder()
        m3Finder.exactSearch.isSelected = true
        m3Finder.ixNr.value = M3Controller().getIndexUserSelection()[3]
        m3Finder.openModal()
        m3Finder.searchText.text = ""
        m3Finder.searchText.text = "[$todoStatuses]"
        m3Finder.table.refresh()
        m3Finder.table.requestFocus()
    }

    fun showSettings() {
        val settings = find<MG3Settings>()
        settings.openModal()
    }

    suspend fun cancelInvoice() {
        if (checkInvoice()) {
            wizard.invoice.item.status = 8
            wizard.invoice.item.statusText = M3CLIController().getStatusText(wizard.invoice.item.status)
            wizard.invoice.item.finished = true
            saveEntry()
            log(MXLog.LogType.INFO, "Invoice ${wizard.invoice.item.uID} cancelled.")
        }
    }

    suspend fun commissionInvoice() {
        if (checkForCommission()) {
            wizard.invoice.item.status = 1
            wizard.invoice.item.statusText = M3CLIController().getStatusText(wizard.invoice.item.status)
            saveEntry()
            log(MXLog.LogType.INFO, "Invoice ${wizard.invoice.item.uID} commissioned.")
            MXEMailer().sendEMail(
                subject = "Web Shop Order #${wizard.invoice.item.uID}",
                body = "Hey, we're confirming your order over ${wizard.invoice.item.grossTotal} Euro.\n" +
                        "Order Number: #${wizard.invoice.item.uID}\n" +
                        "Date: ${wizard.invoice.item.date}",
                recipient = wizard.invoice.item.buyer
            )
        }
    }

    private fun checkForCommission(): Boolean {
        var valid = false
        if (checkInvoice()) {
            if (wizard.invoice.item.status < 1) {
                valid = true
            } else {
                MGXUserAlert("Invoice already commissioned.").openModal()
            }
        }
        return valid
    }
}
