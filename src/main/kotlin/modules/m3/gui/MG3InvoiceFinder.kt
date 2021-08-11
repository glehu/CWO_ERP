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
import modules.m3.logic.M3Controller
import modules.m3.logic.M3DBManager
import modules.mx.rightButtonsWidth
import modules.mx.logic.MXLog
import modules.mx.m3GlobalIndex
import modules.mx.maxSearchResultsGlobal
import tornadofx.*
import kotlin.system.measureTimeMillis

@ExperimentalSerializationApi
class MG3InvoiceFinder : IModule, View("M3 Invoices")
{
    override fun moduleNameLong() = "MG3InvoiceFinder"
    override fun module() = "M3"
    val db: CwODB by inject()
    private val m3Controller: M3Controller by inject()
    private val m2Controller: M2Controller by inject()
    private var searchText: TextField by singleAssign()
    private var exactSearch: CheckBox by singleAssign()
    private var contactsFound: ObservableList<Invoice> = observableListOf(Invoice(-1))
    private var ixNr = SimpleStringProperty()
    private val ixNrList = FXCollections.observableArrayList(m3GlobalIndex.getIndexUserSelection())!!
    private val threadIDCurrent = SimpleIntegerProperty()
    override val root = borderpane {
        center = form {
            contactsFound.clear()
            threadIDCurrent.value = 0
            fieldset {
                field("Search") {
                    searchText = textfield {
                        textProperty().addListener { _, _, _ ->
                            startSearch()
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
                        m3Controller.showInvoice(it)
                        startSearch()
                    }
                }
            }
        }
        right = vbox {
            //Main functions
            button("New Invoice") {
                action {
                    m3Controller.openWizardNewInvoice()
                    startSearch()
                }
                tooltip("Add a new song to the database.")
                prefWidth = rightButtonsWidth
            }
            //Analytics functions
            button("Analytics") {
                //action { m3Controller.openAnalytics() }
                tooltip("Display a chart to show the distribution of genres.")
                prefWidth = rightButtonsWidth
            }
            //Maintenance functions
            button("Rebuild indices") {
                //TODO: Not yet implemented
                isDisable = true
                tooltip("Rebuilds all indices in case of faulty indices.")
                prefWidth = rightButtonsWidth
            }
            //Data import
            button("Data Import") {
                //TODO: Not yet implemented
                isDisable = true
                //action { m3Controller.openDataImport() }
                tooltip("Import contact data from a .csv file.")
                prefWidth = rightButtonsWidth
            }
        }
    }

    private fun startSearch()
    {
        runAsync {
            threadIDCurrent.value++
            searchForInvoices(threadIDCurrent.value)
        }
    }

    private fun searchForInvoices(threadID: Int)
    {
        var entriesFound = 0
        val timeInMillis = measureTimeMillis {
            val dbManager = M3DBManager()
            contactsFound.clear()
            db.getEntriesFromSearchString(
                searchText.text.uppercase(),
                ixNr.value.substring(0, 1).toInt(),
                exactSearch.isSelected,
                module(),
                maxSearchResultsGlobal,
                m3GlobalIndex
            ) { _, bytes ->
                //Add the contacts to the table
                if (threadID == threadIDCurrent.value)
                {
                    contactsFound.add(dbManager.decodeEntry(bytes) as Invoice)
                    entriesFound++
                }
            }
        }
        if (threadID == threadIDCurrent.value)
        {
            MXLog.log(
                module(), MXLog.LogType.INFO, "$entriesFound invoices loaded (in $timeInMillis ms)",
                moduleNameLong()
            )
        }
    }
}