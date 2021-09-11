package modules.m1.gui

import interfaces.IEntry
import interfaces.IEntryFinder
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.Song
import modules.m1.logic.M1Controller
import modules.mx.m1GlobalIndex
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class MG1EntryFinder : IModule, IEntryFinder, View("M1 Discography") {
    override val moduleNameLong = "MG1EntryFinder"
    override val module = "M1"
    override fun getIndexManager(): IIndexManager {
        return m1GlobalIndex
    }
    override var searchText: TextField by singleAssign()
    override var exactSearch: CheckBox by singleAssign()
    override var entriesFound: ObservableList<IEntry> = observableListOf()
    override var ixNr = SimpleStringProperty()
    override val ixNrList: ObservableList<String> = observableArrayList(getIndexUserSelection())
    override val threadIDCurrentProperty = SimpleIntegerProperty()
    private val m1Controller: M1Controller by inject()
    override val root = borderpane {
        center = form {
            prefWidth = 1200.0
            threadIDCurrentProperty.value = 0
            fieldset {
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
                }
                @Suppress("UNCHECKED_CAST")
                tableview(entriesFound as ObservableList<Song>) {
                    readonlyColumn("ID", Song::uID).prefWidth(65.0)
                    readonlyColumn("Name", Song::name).prefWidth(310.0)
                    readonlyColumn("Vocalist", Song::vocalist).prefWidth(200.0)
                    readonlyColumn("Producer", Song::producer).prefWidth(200.0)
                    readonlyColumn("Genre", Song::genre).prefWidth(200.0)
                    onUserSelect(1) {
                        m1Controller.showEntry(it)
                        searchText.text = ""
                        close()
                    }
                    isFocusTraversable = false
                }
            }
        }
    }
}