package modules.m4.gui

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.M4Storage
import modules.m4.logic.M4StorageManager
import modules.mx.gui.userAlerts.MGXUserAlert
import modules.mx.rightButtonsWidth
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class MG4StorageManager : View("M4 Storage Locations") {
    private val storageManager: M4StorageManager by inject()

    @ExperimentalSerializationApi
    private var storages = storageManager.getStorages()
    private var storagesList = observableListOf<M4Storage>()
    private val table = tableview(storagesList) {
        readonlyColumn("Number", M4Storage::number).prefWidth(100.0)
        readonlyColumn("Description", M4Storage::description).prefWidth(400.0)
        readonlyColumn("Lock", M4Storage::locked).prefWidth(50.0)
            .cellFormat { text = ""; style { backgroundColor = storageManager.getLockedCellColor(it) } }
        onUserSelect(1) {
            if (it.number != 0) {
                storageManager.showCategory(it, storages)
            } else {
                MGXUserAlert(
                    "The default storage cannot be edited.\n\n" +
                            "Please add a new storage location or edit others, if available."
                ).openModal()
            }
        }
        isFocusTraversable = false
    }
    override val root = borderpane {
        refreshStorages()
        center = table
        right = vbox {
            button("Add Storage") {
                action {
                    storageManager.addStorage(storages)
                }
                prefWidth = rightButtonsWidth
            }
        }
    }

    fun refreshStorages() {
        storages = storageManager.getStorages()
        storagesList = storageManager.getStorages(storages)
        table.items = storagesList
        table.refresh()
    }
}
