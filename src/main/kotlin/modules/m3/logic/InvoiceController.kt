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
import modules.m4.logic.ItemController
import modules.m4.logic.ItemPriceManager
import modules.m4stockposting.logic.ItemStockPostingController
import modules.mx.gui.userAlerts.GAlert
import modules.mx.invoiceIndexManager
import modules.mx.logic.Emailer
import modules.mx.logic.Log
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
    val vatPercent = (categories.priceCategories[invoice.priceCategory]?.vatPercent) ?: 0.0
    invoice.grossTotal = 0.0
    for (item in invoice.itemsProperty) {
      //Invoice specific calculation
      invoice.grossTotal += (item.grossPrice * item.amount)
      //Line specific calculation
      invoice.itemsProperty[pos].netPrice = InvoiceCLIController().getNetFromGross(item.grossPrice, vatPercent)
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
        commitStockPosting(
          itemUID = item.uID,
          storageFromUID = item.storageFrom1UID,
          storageUnitFromUID = item.storageUnitFrom1UID,
          storageAmount = item.storageAmount1,
          stockPostingsFromUID = item.stockPostingsFrom1UID
        )
        commitStockPosting(
          itemUID = item.uID,
          storageFromUID = item.storageFrom2UID,
          storageUnitFromUID = item.storageUnitFrom2UID,
          storageAmount = item.storageAmount2,
          stockPostingsFromUID = item.stockPostingsFrom2UID
        )
        commitStockPosting(
          itemUID = item.uID,
          storageFromUID = item.storageFrom3UID,
          storageUnitFromUID = item.storageUnitFrom3UID,
          storageAmount = item.storageAmount3,
          stockPostingsFromUID = item.stockPostingsFrom3UID
        )
      }
      wizard.invoice.item.status = 9
      wizard.invoice.item.statusText = InvoiceCLIController().getStatusText(wizard.invoice.item.status)
      wizard.invoice.item.finished = true
      saveEntry()
    }
  }

  private fun commitStockPosting(
    itemUID: Int,
    storageFromUID: Int,
    storageUnitFromUID: Int,
    storageAmount: Double,
    stockPostingsFromUID: Map<Int, Double>
  ) {
    if (storageFromUID == -1 || storageUnitFromUID == -1) return
    if (stockPostingsFromUID.isEmpty()) {
      ItemController().postStock(
        itemUID = itemUID,
        storageFromUID = storageFromUID,
        storageUnitFromUID = storageUnitFromUID,
        stockPostingFromUID = -1,
        storageToUID = -1,
        storageUnitToUID = -1,
        amount = storageAmount,
        note = "inv${wizard.invoice.uID.value}"
      )
    } else {
      for (stockPosting in stockPostingsFromUID) {
        ItemController().postStock(
          itemUID = itemUID,
          storageFromUID = storageFromUID,
          storageUnitFromUID = storageUnitFromUID,
          stockPostingFromUID = stockPosting.key,
          storageToUID = -1,
          storageUnitToUID = -1,
          amount = stockPosting.value,
          note = "inv${wizard.invoice.uID.value}"
        )
      }
    }
  }

  suspend fun payInvoice() {
    if (checkInvoice() && checkForSetPaid()) {
      val paymentScreen = find<GInvoicePayer>()
      paymentScreen.setAmountToPay(wizard.invoice.item.grossTotal)
      paymentScreen.openModal(block = true)
      if (paymentScreen.userConfirmed) {
        wizard.invoice.item.paidGross = paymentScreen.paidAmount.value
        wizard.invoice.item.paidNet = InvoiceCLIController()
          .getNetFromGross(wizard.invoice.item.netTotal, 0.19)
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
        "Please fill out the invoice completely.\n\n" + "Missing fields are marked red."
      ).openModal()
    }
    return valid
  }

  fun showToDoInvoices() {
    val iniVal = InvoiceCLIController().getIni()
    val m3Finder = GInvoiceFinder()
    m3Finder.openModal()
    m3Finder.modalSearch("[${iniVal.todoStatuses}]", 4)
  }

  fun showSettings() {
    val settings = find<GInvoiceSettings>()
    settings.openModal()
  }

  suspend fun cancelInvoice() {
    if (checkInvoice()) {
      val prompt = GAlert(
        "This action will cancel the invoice. Continue?", true
      )
      prompt.openModal(block = true)
      if (prompt.confirmed.value) {
        wizard.invoice.item.status = 8
        wizard.invoice.item.statusText = InvoiceCLIController().getStatusText(wizard.invoice.item.status)
        wizard.invoice.item.finished = true
        saveEntry()
        log(Log.Type.INFO, "Invoice ${wizard.invoice.item.uID} cancelled.")
      }
    }
  }

  suspend fun commissionInvoice() {
    if (checkForCommission()) {
      wizard.invoice.item.status = 1
      wizard.invoice.item.statusText = InvoiceCLIController().getStatusText(wizard.invoice.item.status)
      saveEntry()
      log(Log.Type.INFO, "Invoice ${wizard.invoice.item.uID} commissioned.")
      Emailer().sendEmail(
        subject = "Web Shop Order #${wizard.invoice.item.uID}",
        body = "Hey, we're confirming your order over ${wizard.invoice.item.grossTotal} Euro.\n" + "Order Number: #${wizard.invoice.item.uID}\n" + "Date: ${wizard.invoice.item.date}",
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

  fun checkStorageUnits() {
    for (pos in wizard.invoice.items.value) {
      //Check if we need to check the storages
      if (pos.amount == 0.0) {
        //Reset storage units
        pos.storageFrom1UID = -1; pos.storageUnitFrom1UID = -1; pos.stockPostingsFrom1UID.clear()
        pos.storageFrom2UID = -1; pos.storageUnitFrom2UID = -1; pos.stockPostingsFrom2UID.clear()
        pos.storageFrom3UID = -1; pos.storageUnitFrom3UID = -1; pos.stockPostingsFrom3UID.clear()
      } else {
        //Try to find (a) new storage unit(s)
        //Scenario: A purchase has been made before this request
        //Solution: Try finding a stock posting that could fit it all at once
        val uIDs = ItemStockPostingController().getStorageUnitWithAtLeast(pos.amount)
        if (uIDs.first != -1) {
          pos.storageFrom1UID = uIDs.first
          pos.storageUnitFrom1UID = uIDs.second
          pos.stockPostingsFrom1UID[uIDs.third] = pos.amount
          pos.storageAmount1 = pos.amount
        } else {
          //Scenario: We have to split the amount into multiple bookings
          //          (Up to three storage units, but unlimited stock postings on the same storage unit)
          //Solution: Check which storages have available stock
          val storagesFrom = ItemStockPostingController().getStorageUnitsWithStock(pos.amount)
          pos.stockPostingsFrom1UID.clear()
          pos.storageAmount1 = 0.0
          if (storagesFrom[0].isNotEmpty()) {
            pos.storageFrom1UID = storagesFrom[0][0].first.storageToUID
            pos.storageUnitFrom1UID = storagesFrom[0][0].first.storageUnitToUID
            for (stockPosting in storagesFrom[0]) {
              pos.stockPostingsFrom1UID[stockPosting.first.uID] = stockPosting.second
              pos.storageAmount1 += stockPosting.second
            }
          }
          pos.stockPostingsFrom2UID.clear()
          pos.storageAmount2 = 0.0
          if (storagesFrom[1].isNotEmpty()) {
            pos.storageFrom2UID = storagesFrom[1][0].first.storageToUID
            pos.storageUnitFrom2UID = storagesFrom[1][0].first.storageUnitToUID
            for (stockPosting in storagesFrom[1]) {
              pos.stockPostingsFrom2UID[stockPosting.first.uID] = stockPosting.second
              pos.storageAmount2 += stockPosting.second
            }
          }
          pos.stockPostingsFrom3UID.clear()
          pos.storageAmount3 = 0.0
          if (storagesFrom[2].isNotEmpty()) {
            pos.storageFrom3UID = storagesFrom[2][0].first.storageToUID
            pos.storageUnitFrom3UID = storagesFrom[2][0].first.storageUnitToUID
            for (stockPosting in storagesFrom[2]) {
              pos.stockPostingsFrom3UID[stockPosting.first.uID] = stockPosting.second
              pos.storageAmount3 += stockPosting.second
            }
          }
        }
      }
    }
  }
}
