package modules.m3.logic

import interfaces.IController
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.Contact
import modules.m2.logic.ContactController
import modules.m3.Invoice
import modules.m3.gui.GInvoiceFinder
import modules.m3.gui.GInvoicePayer
import modules.m3.gui.GInvoiceSettings
import modules.m3.gui.InvoiceConfiguratorWizard
import modules.m3.misc.InvoiceProperty
import modules.m3.misc.getInvoiceFromInvoiceProperty
import modules.m3.misc.getInvoicePropertyFromInvoice
import modules.m4.ItemStockPosting
import modules.m4.logic.ItemPriceManager
import modules.mx.gui.userAlerts.GAlert
import modules.mx.logic.EMailer
import modules.mx.logic.Log
import modules.mx.logic.Timestamp
import modules.mx.logic.roundTo
import modules.mx.invoiceIndexManager
import modules.mx.itemStockPostingIndexManager
import tornadofx.Controller

@InternalAPI
@ExperimentalSerializationApi
class InvoiceController : IController, Controller() {
    override val moduleNameLong = "InvoiceController"
    override val module = "M3"
    override fun getIndexManager(): IIndexManager {
        return invoiceIndexManager!!
    }

    private val wizard = find<InvoiceConfiguratorWizard>()

    override fun searchEntry() {
        find<GInvoiceFinder>().openModal()
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
        val entry = get(uID) as Invoice
        wizard.invoice.item = getInvoicePropertyFromInvoice(entry)
    }

    fun calculate(invoice: InvoiceProperty) {
        var pos = 0
        val categories = ItemPriceManager().getCategories()
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
            var contact: Contact
            if (wizard.invoice.item.buyerUID != -1) {
                contact = ContactController().get(wizard.invoice.item.buyerUID) as Contact
                contact.moneySent += wizard.invoice.item.paidGross
                ContactController().save(contact)
            }
            if (wizard.invoice.item.sellerUID != -1) {
                contact = ContactController().get(wizard.invoice.item.sellerUID) as Contact
                contact.moneyReceived += wizard.invoice.item.paidNet
                ContactController().save(contact)
            }
            //Stock Posting
            for (item in wizard.invoice.item.itemsProperty) {
                if (item.stockPostingUID == -1) {
                    val stockPosting =
                        ItemStockPosting(
                            uID = -1,
                            itemUID = item.uID,
                            storageUnitFromUID = -1,
                            storageUnitToUID = -1,
                            amount = item.amount,
                            date = Timestamp.now()
                        )
                    stockPosting.book()
                    itemStockPostingIndexManager!!.save(stockPosting)
                }
            }
            wizard.invoice.item.status = 9
            wizard.invoice.item.statusText = InvoiceCLIController().getStatusText(wizard.invoice.item.status)
            wizard.invoice.item.finished = true
            saveEntry()
        }
    }

    suspend fun payInvoice() {
        if (checkInvoice() && checkForSetPaid()) {
            val paymentScreen = find<GInvoicePayer>()
            paymentScreen.openModal(block = true)
            if (paymentScreen.userConfirmed) {
                wizard.invoice.item.paidGross = wizard.invoice.item.grossTotal
                wizard.invoice.item.paidNet = wizard.invoice.item.netTotal
                if (wizard.invoice.item.paidGross == wizard.invoice.item.grossTotal) {
                    wizard.invoice.item.status = 3
                } else {
                    wizard.invoice.item.status = 2
                }
                wizard.invoice.item.statusText = InvoiceCLIController().getStatusText(wizard.invoice.item.status)
                saveEntry()
            }
        }
    }

    private fun checkForSetPaid(): Boolean {
        var valid = false
        if (wizard.invoice.item.paidGross < wizard.invoice.item.grossTotal) {
            valid = true
        } else {
            GAlert("Invoice is already paid.").openModal()
        }
        return valid
    }

    private fun checkForProcess(): Boolean {
        var valid = false
        if (wizard.invoice.item.paidGross == wizard.invoice.item.grossTotal) {
            valid = true
        } else if (wizard.invoice.item.paidGross < wizard.invoice.item.grossTotal) {
            GAlert("Paid amount is less than invoice total.").openModal()
        } else if (wizard.invoice.item.paidGross > wizard.invoice.item.grossTotal) {
            GAlert("Paid amount is more than invoice total.").openModal()
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
                GAlert("The invoice is already finished.").openModal()
            }
        } else {
            GAlert(
                "Please fill out the invoice completely.\n\n" +
                        "Missing fields are marked red."
            ).openModal()
        }
        return valid
    }

    fun showToDoInvoices() {
        val iniVal = InvoiceCLIController().getIni()
        val todoStatuses = iniVal.todoStatuses
        val m3Finder = GInvoiceFinder()
        m3Finder.exactSearch.isSelected = true
        m3Finder.ixNr.value = InvoiceController().getIndexUserSelection()[3]
        m3Finder.openModal()
        m3Finder.searchText.text = ""
        m3Finder.searchText.text = "[$todoStatuses]"
        m3Finder.table.refresh()
        m3Finder.table.requestFocus()
    }

    fun showSettings() {
        val settings = find<GInvoiceSettings>()
        settings.openModal()
    }

    suspend fun cancelInvoice() {
        if (checkInvoice()) {
            wizard.invoice.item.status = 8
            wizard.invoice.item.statusText = InvoiceCLIController().getStatusText(wizard.invoice.item.status)
            wizard.invoice.item.finished = true
            saveEntry()
            log(Log.LogType.INFO, "Invoice ${wizard.invoice.item.uID} cancelled.")
        }
    }

    suspend fun commissionInvoice() {
        if (checkForCommission()) {
            wizard.invoice.item.status = 1
            wizard.invoice.item.statusText = InvoiceCLIController().getStatusText(wizard.invoice.item.status)
            saveEntry()
            log(Log.LogType.INFO, "Invoice ${wizard.invoice.item.uID} commissioned.")
            EMailer().sendEMail(
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
                GAlert("Invoice already commissioned.").openModal()
            }
        }
        return valid
    }
}
