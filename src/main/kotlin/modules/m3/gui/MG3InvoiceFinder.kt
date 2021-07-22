package modules.m3.gui

import db.CwODB
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import kotlinx.serialization.ExperimentalSerializationApi
import modules.IModule
import modules.m2.logic.M2Controller
import modules.m3.Invoice
import modules.m3.logic.M3DBManager
import modules.m3.logic.M3IndexManager
import modules.m3.misc.InvoiceProperty
import modules.m3.misc.getInvoiceFromInvoiceProperty
import modules.m3.misc.getInvoicePropertyFromInvoice
import modules.mx.logic.MXLog
import modules.mx.maxSearchResultsGlobal
import tornadofx.*
import kotlin.system.measureTimeMillis

@ExperimentalSerializationApi
class MG3InvoiceFinder : IModule, View("Find Invoice")
{
    override fun moduleNameLong() = "MG3InvoiceFinder"
    override fun module() = "M3"
    val db: CwODB by inject()
    val indexManager: M3IndexManager by inject()
    private val m2Controller: M2Controller by inject()
    private var contactName: TextField by singleAssign()
    private var exactSearch: CheckBox by singleAssign()
    private var contactsFound: ObservableList<Invoice> = observableList(Invoice(-1))
    private var ixNr = SimpleStringProperty()
    private val ixNrList = FXCollections.observableArrayList(indexManager.getIndexUserSelection())!!
    private val threadIDCurrent = SimpleIntegerProperty()
    override val root = form {
        contactsFound.clear()
        threadIDCurrent.value = 0
        fieldset {
            field("Search text") {
                contactName = textfield {
                    textProperty().addListener { _, _, _ ->
                        runAsync {
                            threadIDCurrent.value++
                            searchForInvoices(threadIDCurrent.value)
                        }
                    }
                    tooltip("Contains the search text that will be used to find an entry.")
                }
                exactSearch = checkbox("Exact Search") {
                    tooltip("If checked, a literal search will be done.")
                }
            }
            fieldset("Index")
            {
                ixNr.value = "1-Name"
                combobox(ixNr, ixNrList) {
                    tooltip("Selects the index file that will be searched in.")
                }
            }
            tableview(contactsFound) {
                readonlyColumn("ID", Invoice::uID).prefWidth(65.0)
                readonlyColumn("Seller", Invoice::seller).prefWidth(350.0).cellFormat {
                    text = m2Controller.getContactName(rowItem.sellerUID, rowItem.seller)
                    rowItem.seller = text
                }
                readonlyColumn("Buyer", Invoice::buyer).prefWidth(350.0).cellFormat {
                    text = m2Controller.getContactName(rowItem.buyerUID, rowItem.buyer)
                    rowItem.buyer = text
                }
                readonlyColumn("Text", Invoice::text).prefWidth(200.0)
                onUserSelect(1) {
                    showInvoice(it)
                    contactsFound.clear()
                    contactName.text = ""
                    close()
                }
            }
        }
    }

    private fun searchForInvoices(threadID: Int)
    {
        var entriesFound = 0
        val timeInMillis = measureTimeMillis {
            val dbManager = M3DBManager()
            contactsFound.clear()
            db.getEntriesFromSearchString(
                contactName.text.uppercase(),
                ixNr.value.substring(0, 1).toInt(),
                exactSearch.isSelected,
                module(),
                maxSearchResultsGlobal,
                indexManager
            ) { _, bytes ->
                //Add the contacts to the table
                if (threadID >= threadIDCurrent.value)
                {
                    contactsFound.add(dbManager.decodeEntry(bytes) as Invoice)
                    entriesFound++
                }
            }
        }
        if (threadID >= threadIDCurrent.value)
        {
            MXLog.log(
                module(), MXLog.LogType.INFO, "$entriesFound invoices loaded (in $timeInMillis ms)",
                moduleNameLong()
            )
        }
    }

    private fun showInvoice(invoice: Invoice)
    {
        val wizard = find<InvoiceViewerWizard>()
        wizard.invoice.item = getInvoicePropertyFromInvoice(invoice)
        wizard.onComplete {
            if (wizard.invoice.item !== null)
            {
                val raf = db.openRandomFileAccess(module(), "rw")
                M3DBManager().saveEntry(
                    entry = getInvoiceFromInvoiceProperty(wizard.invoice.item),
                    cwodb = db,
                    posDB = indexManager.indexList[0]!!.indexMap[wizard.invoice.item.uID]!!.pos,
                    byteSize = indexManager.indexList[0]!!.indexMap[wizard.invoice.item.uID]!!.byteSize,
                    raf = raf,
                    indexManager = indexManager
                )
                this.db.closeRandomFileAccess(raf)
                wizard.invoice.item = InvoiceProperty()
                wizard.isComplete = false
                wizard.close()
            }
        }

        wizard.openModal()
    }
}