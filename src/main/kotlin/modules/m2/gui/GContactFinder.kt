package modules.m2.gui

import components.gui.tornadofx.entryfinder.EntryFinderSearchMask
import interfaces.IEntry
import interfaces.IEntryFinder
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.misc.SongPropertyMainDataModel
import modules.m2.Contact
import modules.m2.logic.ContactController
import modules.mx.contactIndexManager
import modules.mx.gui.userAlerts.GAlertLocked
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class GContactFinder : IModule, IEntryFinder, View("Contact Finder") {
    override val moduleNameLong = "ContactFinder"
    override val module = "M2"
    override fun getIndexManager(): IIndexManager {
        return contactIndexManager!!
    }

    override var searchText: TextField by singleAssign()
    override var exactSearch: CheckBox by singleAssign()
    override var entriesFound: ObservableList<IEntry> = observableListOf()
    override var ixNr = SimpleStringProperty()
    override val ixNrList: ObservableList<String> = observableArrayList(getIndexUserSelection())
    override val entryFinderSearchMask: EntryFinderSearchMask =
        EntryFinderSearchMask(origin = this, ixManager = getIndexManager())

    private val contactController: ContactController by inject()
    private val transfer: SongPropertyMainDataModel by inject()

    @Suppress("UNCHECKED_CAST")
    override val table = tableview(entriesFound as ObservableList<Contact>) {
        readonlyColumn("ID", Contact::uID)
        readonlyColumn("Name", Contact::name).remainingWidth()
        readonlyColumn("EMail", Contact::email)
        readonlyColumn("F.Name", Contact::firstName)
        readonlyColumn("City", Contact::city)
        onUserSelect(1) {
            if (transfer.uID.value == -2) {
                //Data transfer
                transfer.uID.value = it.uID
                transfer.name.value = it.name
                transfer.commit()
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
    } as TableView<IEntry>

    override val root = form {
        add(entryFinderSearchMask.searchMask)
        add(table)
    }
}
