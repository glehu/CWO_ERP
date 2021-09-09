package modules.m2.gui

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
import modules.m1.misc.SongPropertyMainDataModel
import modules.m2.Contact
import modules.m2.logic.M2Controller
import modules.mx.m2GlobalIndex
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class MG2ContactFinder : IModule, IEntryFinder, View("M2 Contacts") {
    override fun moduleNameLong() = "MG2ContactFinder"
    override fun module() = "M2"
    private val m2Controller: M2Controller by inject()
    private val song: SongPropertyMainDataModel by inject()
    override var searchText: TextField by singleAssign()
    override var exactSearch: CheckBox by singleAssign()
    override var entriesFound: ObservableList<IEntry> = observableListOf()
    override var ixNr = SimpleStringProperty()
    override val ixNrList = FXCollections.observableArrayList(m2Controller.getIndexUserSelection())!!
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
                                searchForEntries(threadIDCurrentProperty.value, m2GlobalIndex)
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
                tableview(entriesFound as ObservableList<Contact>) {
                    readonlyColumn("ID", Contact::uID).prefWidth(65.0)
                    readonlyColumn("Name", Contact::name).prefWidth(350.0)
                    readonlyColumn("F.Name", Contact::firstName).prefWidth(250.0)
                    readonlyColumn("City", Contact::city).prefWidth(200.0)
                    onUserSelect(1) {
                        if (song.uID.value == -2) {
                            //Data transfer
                            song.uID.value = it.uID
                            song.name.value = it.name
                            song.commit()
                            close()
                        } else {
                            m2Controller.showContact(it)
                            searchText.text = ""
                            close()
                        }
                    }
                    isFocusTraversable = false
                }
            }
        }
    }
}