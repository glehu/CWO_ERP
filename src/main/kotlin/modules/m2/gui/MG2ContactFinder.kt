package modules.m2.gui

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
import modules.m1.misc.SongPropertyMainDataModel
import modules.m2.M2Contact
import modules.m2.logic.M2Controller
import modules.mx.gui.userAlerts.MGXLocked
import modules.mx.m2GlobalIndex
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class MG2ContactFinder : IModule, IEntryFinder, View("M2 Contacts") {
    override val moduleNameLong = "MG2ContactFinder"
    override val module = "M2"
    override fun getIndexManager(): IIndexManager {
        return m2GlobalIndex!!
    }

    override var searchText: TextField by singleAssign()
    override var exactSearch: CheckBox by singleAssign()
    override var entriesFound: ObservableList<IEntry> = observableListOf()
    override var ixNr = SimpleStringProperty()
    override val ixNrList: ObservableList<String> = FXCollections.observableArrayList(getIndexUserSelection())
    override val threadIDCurrentProperty = SimpleIntegerProperty()
    private val m2Controller: M2Controller by inject()
    private val song: SongPropertyMainDataModel by inject()
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
                tableview(entriesFound as ObservableList<M2Contact>) {
                    readonlyColumn("ID", M2Contact::uID).prefWidth(65.0)
                    readonlyColumn("Name", M2Contact::name).prefWidth(350.0)
                    readonlyColumn("F.Name", M2Contact::firstName).prefWidth(250.0)
                    readonlyColumn("City", M2Contact::city).prefWidth(200.0)
                    onUserSelect(1) {
                        if (song.uID.value == -2) {
                            //Data transfer
                            song.uID.value = it.uID
                            song.name.value = it.name
                            song.commit()
                            close()
                        } else {
                            if (!getEntryLock(it.uID)) {
                                m2Controller.showEntry(it.uID)
                                searchText.text = ""
                                close()
                            } else {
                                find<MGXLocked>().openModal()
                            }
                        }
                    }
                    isFocusTraversable = false
                }
            }
        }
    }
}
