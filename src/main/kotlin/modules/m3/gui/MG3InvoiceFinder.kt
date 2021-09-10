package modules.m3.gui

import interfaces.IEntry
import interfaces.IEntryFinder
import interfaces.IModule
import io.ktor.util.*
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m3.Invoice
import modules.m3.logic.M3Controller
import modules.mx.m3GlobalIndex
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class MG3InvoiceFinder : IModule, IEntryFinder, View("M3 Invoices") {
    override val moduleNameLong = "MG3InvoiceFinder"
    override val module = "M3"
    private val m3Controller: M3Controller by inject()
    override var searchText: TextField by singleAssign()
    override var exactSearch: CheckBox by singleAssign()
    override var entriesFound: ObservableList<IEntry> = observableListOf()
    override var ixNr = SimpleStringProperty()
    override val ixNrList = FXCollections.observableArrayList(m3Controller.getIndexUserSelection())!!
    override val threadIDCurrentProperty = SimpleIntegerProperty()
    override val root = borderpane {
        center = form {
            prefWidth = 1200.0
            fieldset {
                field("Search") {
                    searchText = textfield {
                        textProperty().addListener { _, _, _ ->
                            runAsync {
                                threadIDCurrentProperty.value++
                                searchForEntries(threadIDCurrentProperty.value, m3GlobalIndex)
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
                tableview(entriesFound as ObservableList<Invoice>) {
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
}