package modules.m3.gui

import components.gui.tornadofx.entryfinder.EntryFinder
import interfaces.IEntry
import interfaces.IEntryFinder
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m3.Invoice
import modules.m3.logic.InvoiceController
import modules.mx.gui.userAlerts.GAlertLocked
import modules.mx.m3GlobalIndex
import tornadofx.*

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
    private val entryFinder = EntryFinder(origin = this, ixManager = getIndexManager())

    private val invoiceController: InvoiceController by inject()

    @Suppress("UNCHECKED_CAST")
    override val table = tableview(entriesFound as ObservableList<Invoice>) {
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
    } as TableView<IEntry>

    override val root = form {
        add(entryFinder.searchMask)
        add(table)
    }
}
