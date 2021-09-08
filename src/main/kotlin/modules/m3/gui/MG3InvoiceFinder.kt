package modules.m3.gui

import api.logic.getCWOClient
import api.misc.json.M1EntryListJson
import db.CwODB
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.client.request.*
import io.ktor.util.*
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m3.Invoice
import modules.m3.logic.M3Controller
import modules.m3.logic.M3DBManager
import modules.mx.activeUser
import modules.mx.isClientGlobal
import modules.mx.logic.MXLog
import modules.mx.logic.indexFormat
import modules.mx.m3GlobalIndex
import modules.mx.maxSearchResultsGlobal
import tornadofx.*
import kotlin.system.measureTimeMillis

@InternalAPI
@ExperimentalSerializationApi
class MG3InvoiceFinder : IModule, View("M3 Invoices") {
    override fun moduleNameLong() = "MG3InvoiceFinder"
    override fun module() = "M3"
    val db: CwODB by inject()
    private val m3Controller: M3Controller by inject()
    private var searchText: TextField by singleAssign()
    private var exactSearch: CheckBox by singleAssign()
    private var invoicesFound: ObservableList<Invoice> = observableListOf(Invoice(-1))
    private var ixNr = SimpleStringProperty()
    private val ixNrList = FXCollections.observableArrayList(m3Controller.getIndexUserSelection())!!
    private val threadIDCurrentProperty = SimpleIntegerProperty()
    private var threadIDCurrent by threadIDCurrentProperty
    override val root = borderpane {
        center = form {
            prefWidth = 1200.0
            invoicesFound.clear()
            threadIDCurrent = 0
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
                    ixNr.value = ixNrList[0]
                    combobox(ixNr, ixNrList) {
                        tooltip("Selects the index file that will be searched in.")
                    }
                }
                tableview(invoicesFound) {
                    readonlyColumn("ID", Invoice::uID).prefWidth(65.0)
                    readonlyColumn("Seller", Invoice::seller).prefWidth(300.0)
                    readonlyColumn("Buyer", Invoice::buyer).prefWidth(300.0)
                    readonlyColumn("Text", Invoice::text).prefWidth(400.0)
                    onUserSelect(1) {
                        m3Controller.showInvoice(it)
                        searchText.text = ""
                        close()
                    }
                    isFocusTraversable = false
                }
            }
        }
    }

    private fun startSearch() {
        runAsync {
            threadIDCurrent++
            searchForInvoices(threadIDCurrent)
        }
    }

    private fun searchForInvoices(threadID: Int) {
        var entriesFound = 0
        val dbManager = M3DBManager()
        val timeInMillis = measureTimeMillis {
            if (!isClientGlobal) {
                invoicesFound.clear()
                db.getEntriesFromSearchString(
                    indexFormat(searchText.text),
                    ixNr.value.substring(0, 1).toInt(),
                    exactSearch.isSelected,
                    module(),
                    maxSearchResultsGlobal,
                    m3GlobalIndex
                ) { _, bytes ->
                    //Add the contacts to the table
                    if (threadID == threadIDCurrent) {
                        invoicesFound.add(dbManager.decodeEntry(bytes) as Invoice)
                        entriesFound++
                    }
                }
            } else if (isClientGlobal) {
                if (searchText.text.isNotEmpty()) {
                    runBlocking {
                        launch {
                            val entryListJson: M1EntryListJson = getCWOClient(activeUser.username, activeUser.password)
                                .get(
                                    getApiUrl() +
                                            "entry/${indexFormat(searchText.text)}" +
                                            "?type=name"
                                )
                            if (threadID == threadIDCurrent) {
                                this@MG3InvoiceFinder.invoicesFound.clear()
                                for (entryBytes: ByteArray in entryListJson.resultsList) {
                                    entriesFound++
                                    this@MG3InvoiceFinder.invoicesFound.add(
                                        dbManager.decodeEntry(entryBytes) as Invoice
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (threadID == threadIDCurrent) {
            MXLog.log(
                module(), MXLog.LogType.INFO, "$entriesFound invoices loaded (in $timeInMillis ms)",
                moduleNameLong()
            )
        }
    }
}