@file:Suppress("DuplicatedCode")

package modules.m1.gui

import components.gui.tornadofx.entryfinder.EntryFinderSearchMask
import interfaces.IEntry
import interfaces.IEntryFinder
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.Song
import modules.m1.logic.DiscographyController
import modules.mx.discographyIndexManager
import modules.mx.gui.userAlerts.GAlertLocked
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class GDiscographyFinder : IModule, IEntryFinder, View("Discography Finder") {
    override val moduleNameLong = "DiscographyFinder"
    override val module = "M1"
    override fun getIndexManager(): IIndexManager {
        return discographyIndexManager!!
    }

    override var searchText: TextField by singleAssign()
    override var exactSearch: CheckBox by singleAssign()
    override var entriesFound: ObservableList<IEntry> = observableListOf()
    override var ixNr = SimpleStringProperty()
    override val ixNrList: ObservableList<String> = FXCollections.observableArrayList(getIndexUserSelection())
    override val entryFinderSearchMask: EntryFinderSearchMask =
        EntryFinderSearchMask(origin = this, ixManager = tryGetIndexManager())

    private val discographyController: DiscographyController by inject()

    @Suppress("UNCHECKED_CAST")
    override val table = tableview(entriesFound as ObservableList<Song>) {
        readonlyColumn("ID", Song::uID)
        readonlyColumn("Name", Song::name).remainingWidth()
        readonlyColumn("Vocalist", Song::vocalist)
        readonlyColumn("Producer", Song::producer)
        readonlyColumn("Genre", Song::genre)
        readonlyColumn("Type", Song::type)
        onUserSelect(1) {
            if (!getEntryLock(it.uID)) {
                discographyController.showEntry(it.uID)
                close()
            } else {
                find<GAlertLocked>().openModal()
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
