package components.gui.tornadofx.entryfinder

import interfaces.IEntry
import interfaces.IEntryFinder
import interfaces.IIndexManager
import io.ktor.util.*
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import tornadofx.*

@ExperimentalSerializationApi
@InternalAPI
class EntryFinder(
    val origin: IEntryFinder,
    val ixManager: IIndexManager
) : IEntryFinder, View() {
    override val moduleNameLong = origin.moduleNameLong
    override val module = origin.module
    override fun getIndexManager(): IIndexManager {
        return ixManager
    }

    override var searchText: TextField by singleAssign()
    override var exactSearch: CheckBox by singleAssign()
    override var entriesFound: ObservableList<IEntry> = observableListOf()
    override var ixNr = SimpleStringProperty()
    override val ixNrList: ObservableList<String> = observableArrayList(origin.getIndexUserSelection())
    override val table: TableView<IEntry> = TableView<IEntry>()

    private var job: Job = Job()

    val searchMask = borderpane {
        center = form {
            prefWidth = 1200.0
            fieldset {
                fieldset {
                    field("Search") {
                        searchText = textfield {
                            textProperty().addListener { _, _, _ ->
                                job.cancel()
                                runAsync {
                                    runBlocking {
                                        job = launch {
                                            origin.searchForEntries(
                                                entryFinder = this@EntryFinder
                                            )
                                            origin.table.refresh()
                                            origin.table.requestResize()
                                        }
                                    }
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
            }
        }
    }
    override val root = searchMask
}
