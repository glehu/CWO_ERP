package modules.m3.gui

import interfaces.IEntry
import interfaces.IEntryFinder
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.*
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m3.Invoice
import modules.m3.logic.InvoiceController
import modules.mx.gui.userAlerts.GAlertLocked
import modules.mx.m3GlobalIndex
import tornadofx.*
import java.net.InetSocketAddress

@InternalAPI
@ExperimentalSerializationApi
class GInvoiceFinder : IModule, IEntryFinder, View("M3 Invoices") {
    override val moduleNameLong = "MG3InvoiceFinder"
    override val module = "M3"
    override fun getIndexManager(): IIndexManager {
        return m3GlobalIndex!!
    }

    override var searchText: TextField by singleAssign()
    override var exactSearch: CheckBox by singleAssign()
    override var entriesFound: ObservableList<IEntry> = observableListOf()
    override var ixNr = SimpleStringProperty()
    override val ixNrList: ObservableList<String> = FXCollections.observableArrayList(getIndexUserSelection())
    override val threadIDCurrentProperty = SimpleIntegerProperty()
    private val invoiceController: InvoiceController by inject()

    @Suppress("UNCHECKED_CAST")
    val table = tableview(entriesFound as ObservableList<Invoice>) {
        readonlyColumn("ID", Invoice::uID)
        readonlyColumn("Seller", Invoice::seller)
        readonlyColumn("Buyer", Invoice::buyer)
        readonlyColumn("Price", Invoice::grossTotal)
        readonlyColumn("Date", Invoice::date)
        readonlyColumn("Text", Invoice::text).remainingWidth()
        readonlyColumn("Status", Invoice::status)
        readonlyColumn("SText", Invoice::statusText)
        onUserSelect(1) {
            if (!getEntryLock(it.uID)) {
                invoiceController.showEntry(it.uID)
                close()
            } else {
                find<GAlertLocked>().openModal()
            }
        }
        columnResizePolicy = SmartResize.POLICY
        isFocusTraversable = false
    }
    override val root = borderpane {
        center = form {
            prefWidth = 1200.0
            fieldset {
                field("Search") {
                    searchText = textfield {
                        textProperty().addListener { _, _, _ ->
                            runBlocking {
                                threadIDCurrentProperty.value++
                                searchForEntries(threadIDCurrentProperty.value)
                                table.refresh()
                                table.requestResize()
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
                    ixNr.value = ixNrList[0]
                    combobox(ixNr, ixNrList) {
                        tooltip("Selects the index file that will be searched in.")
                    }
                }
                add(table)
            }
        }
    }
}
