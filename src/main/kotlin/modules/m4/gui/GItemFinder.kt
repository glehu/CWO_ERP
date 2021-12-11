package modules.m4.gui

import interfaces.IEntry
import interfaces.IEntryFinder
import interfaces.IIndexManager
import io.ktor.util.*
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.misc.SongPropertyMainDataModel
import modules.m4.Item
import modules.m4.logic.ItemController
import modules.mx.gui.userAlerts.GAlertLocked
import modules.mx.m4GlobalIndex
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class GItemFinder : IEntryFinder, View("M3 Invoices") {
    override val moduleNameLong = "MG4ItemFinder"
    override val module = "M4"
    override fun getIndexManager(): IIndexManager {
        return m4GlobalIndex!!
    }

    override var searchText: TextField by singleAssign()
    override var exactSearch: CheckBox by singleAssign()
    override var entriesFound: ObservableList<IEntry> = observableListOf()
    override var ixNr = SimpleStringProperty()
    override val ixNrList: ObservableList<String> = FXCollections.observableArrayList(getIndexUserSelection())
    override val threadIDCurrentProperty = SimpleIntegerProperty()
    private val itemController: ItemController by inject()
    private val song: SongPropertyMainDataModel by inject()

    @Suppress("UNCHECKED_CAST")
    val table = tableview(entriesFound as ObservableList<Item>) {
        readonlyColumn("ID", Item::uID)
        readonlyColumn("Description", Item::description).remainingWidth()
        onUserSelect(1) {
            if (song.uID.value == -2) {
                //Data transfer
                song.uID.value = it.uID
                song.name.value = it.description
                song.commit()
                close()
            } else {
                if (!getEntryLock(it.uID)) {
                    itemController.showEntry(it.uID)
                    close()
                } else {
                    find<GAlertLocked>().openModal()
                }
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
