package modules.m3.gui

import interfaces.IEntry
import interfaces.IEntryFinder
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m3.M3Invoice
import modules.m3.logic.M3Controller
import modules.mx.gui.userAlerts.MGXLocked
import modules.mx.m3GlobalIndex
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class MG3InvoiceFinder : IModule, IEntryFinder, View("M3 Invoices") {
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
    private val m3Controller: M3Controller by inject()

    @Suppress("UNCHECKED_CAST")
    val table = tableview(entriesFound as ObservableList<M3Invoice>) {
        readonlyColumn("ID", M3Invoice::uID)
        readonlyColumn("Seller", M3Invoice::seller)
        readonlyColumn("Buyer", M3Invoice::buyer)
        readonlyColumn("Price", M3Invoice::grossTotal)
        readonlyColumn("Date", M3Invoice::date)
        readonlyColumn("Text", M3Invoice::text).remainingWidth()
        readonlyColumn("Status", M3Invoice::status)
        readonlyColumn("SText", M3Invoice::statusText)
        onUserSelect(1) {
            if (!getEntryLock(it.uID)) {
                m3Controller.showEntry(it.uID)
                close()
            } else {
                find<MGXLocked>().openModal()
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
                            runAsync {
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
