package modules.m3Invoices.gui

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
import modules.m3Invoices.M3Invoice
import modules.m3Invoices.logic.M3Controller
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
                @Suppress("UNCHECKED_CAST")
                tableview(entriesFound as ObservableList<M3Invoice>) {
                    readonlyColumn("ID", M3Invoice::uID).prefWidth(65.0)
                    readonlyColumn("Seller", M3Invoice::seller).prefWidth(225.0)
                    readonlyColumn("Buyer", M3Invoice::buyer).prefWidth(225.0)
                    readonlyColumn("Price", M3Invoice::grossPrice).prefWidth(100.0)
                    readonlyColumn("Date", M3Invoice::date).prefWidth(100.0)
                    readonlyColumn("Text", M3Invoice::text).prefWidth(400.0)
                    onUserSelect(1) {
                        if (!getEntryLock(it.uID)) {
                            m3Controller.showEntry(it.uID)
                            searchText.text = ""
                            close()
                        } else {
                            find<MGXLocked>().openModal()
                        }
                    }
                    isFocusTraversable = false
                }
            }
        }
    }
}
