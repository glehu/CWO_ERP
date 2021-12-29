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
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.logic.Log
import tornadofx.*

@ExperimentalSerializationApi
@InternalAPI
class EntryFinderSearchMask(
    val origin: IEntryFinder,
    val ixManager: IIndexManager?
) : IEntryFinder, View() {
    override val moduleNameLong = origin.moduleNameLong
    override val module = origin.module
    override fun getIndexManager(): IIndexManager? {
        return ixManager
    }

    override var searchText: TextField by singleAssign()
    override var exactSearch: CheckBox by singleAssign()
    override var entriesFound: ObservableList<IEntry> = observableListOf()
    override var ixNr = SimpleStringProperty()
    override val ixNrList: ObservableList<String> = observableArrayList(origin.getIndexUserSelection())
    override val table: TableView<IEntry> = TableView<IEntry>()
    override val entryFinderSearchMask: EntryFinderSearchMask = this

    var showAll: CheckBox by singleAssign()
    private var lookupJob: Job = Job()

    val searchMask = borderpane {
        center = form {
            prefWidth = 1200.0
            fieldset {
                field("Search (Enter)") {
                    searchText = textfield {
                        //textProperty().addListener { _, _, _ -> startLookup() }       DISABLED UNTIL THREADSAFE
                        action { startLookup() }
                        tooltip("Contains the search text that will be used to find an entry.")
                    }
                    exactSearch = checkbox("Exact Search") {
                        tooltip("If checked, a literal search will be done.")
                    }
                    showAll = checkbox("Show all") {
                        tooltip("If checked, shows all entries, overriding the max search results settings.")
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

    fun startLookup() {
        runAsync {
            runBlocking {
                try {
                    lookupJob.cancelAndJoin()
                    lookupJob = launch {
                        origin.searchForEntries(entryFinder = this@EntryFinderSearchMask)
                        origin.table.refresh()
                        origin.table.requestResize()
                    }
                } catch (e: CancellationException) {
                    log(Log.LogType.ERROR, e.message ?: "LOOKUP ERR")
                }
            }
        }
    }

    override val root = searchMask
}
