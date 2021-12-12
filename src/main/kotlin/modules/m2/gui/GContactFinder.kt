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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.misc.SongPropertyMainDataModel
import modules.m2.Contact
import modules.m2.logic.ContactController
import modules.mx.gui.userAlerts.GAlertLocked
import modules.mx.m2GlobalIndex
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class GContactFinder : IModule, IEntryFinder, View("M2 Contacts") {
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
    private val contactController: ContactController by inject()
    private val song: SongPropertyMainDataModel by inject()

    @Suppress("UNCHECKED_CAST")
    val table = tableview(entriesFound as ObservableList<Contact>) {
        readonlyColumn("ID", Contact::uID)
        readonlyColumn("Name", Contact::name).remainingWidth()
        readonlyColumn("EMail", Contact::email)
        readonlyColumn("F.Name", Contact::firstName)
        readonlyColumn("City", Contact::city)
        onUserSelect(1) {
            if (song.uID.value == -2) {
                //Data transfer
                song.uID.value = it.uID
                song.name.value = it.name
                song.commit()
                close()
            } else {
                if (!getEntryLock(it.uID)) {
                    contactController.showEntry(it.uID)
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
                            runBlocking {
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
