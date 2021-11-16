@file:Suppress("DuplicatedCode")

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
import modules.m1.M1Song
import modules.m1.logic.M1Controller
import modules.mx.gui.userAlerts.MGXLocked
import modules.mx.m1GlobalIndex
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class MG1EntryFinder : IModule, IEntryFinder, View("M1 Discography") {
    override val moduleNameLong = "MG1EntryFinder"
    override val module = "M1"
    override fun getIndexManager(): IIndexManager {
        return m1GlobalIndex!!
    }

    override var searchText: TextField by singleAssign()
    override var exactSearch: CheckBox by singleAssign()
    override var entriesFound: ObservableList<IEntry> = observableListOf()
    override var ixNr = SimpleStringProperty()
    override val ixNrList: ObservableList<String> = observableArrayList(getIndexUserSelection())
    override val threadIDCurrentProperty = SimpleIntegerProperty()
    private val m1Controller: M1Controller by inject()

    @Suppress("UNCHECKED_CAST")
    val table = tableview(entriesFound as ObservableList<M1Song>) {
        readonlyColumn("ID", M1Song::uID)
        readonlyColumn("Name", M1Song::name).remainingWidth()
        readonlyColumn("Vocalist", M1Song::vocalist)
        readonlyColumn("Producer", M1Song::producer)
        readonlyColumn("Genre", M1Song::genre)
        readonlyColumn("Type", M1Song::type)
        onUserSelect(1) {
            if (!getEntryLock(it.uID)) {
                m1Controller.showEntry(it.uID)
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
            threadIDCurrentProperty.value = 0
            fieldset {
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
                }
                add(table)
            }
        }
    }
}
