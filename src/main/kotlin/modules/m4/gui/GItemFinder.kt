package modules.m4.gui

import components.gui.tornadofx.entryfinder.EntryFinder
import interfaces.IEntry
import interfaces.IEntryFinder
import interfaces.IIndexManager
import io.ktor.util.*
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.TableView
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
    private val entryFinder = EntryFinder(origin = this, ixManager = getIndexManager())

    private val itemController: ItemController by inject()
    private val transfer: SongPropertyMainDataModel by inject()

    @Suppress("UNCHECKED_CAST")
    override val table = tableview(entriesFound as ObservableList<Item>) {
        readonlyColumn("ID", Item::uID)
        readonlyColumn("Description", Item::description).remainingWidth()
        onUserSelect(1) {
            if (transfer.uID.value == -2) {
                //Data transfer
                transfer.uID.value = it.uID
                transfer.name.value = it.description
                transfer.commit()
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
    } as TableView<IEntry>

    override val root = form {
        add(entryFinder.searchMask)
        add(table)
    }
}
