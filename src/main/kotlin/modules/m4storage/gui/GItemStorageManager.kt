package modules.m4storage.gui

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4storage.ItemStorage
import modules.m4storage.logic.ItemStorageManager
import modules.mx.gui.userAlerts.GAlert
import modules.mx.rightButtonsWidth
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class GItemStorageManager : View("Item Storage Locations") {
  private val storageManager: ItemStorageManager by inject()

  @ExperimentalSerializationApi
  private var storages = storageManager.getStorages()
  private var storagesList = observableListOf<ItemStorage>()
  private val table = tableview(storagesList) {
    readonlyColumn("Number", ItemStorage::number).prefWidth(100.0)
    readonlyColumn("Description", ItemStorage::description).prefWidth(400.0)
    readonlyColumn("Lock", ItemStorage::locked).prefWidth(50.0)
      .cellFormat { text = ""; style { backgroundColor = storageManager.getLockedCellColor(it) } }
    onUserSelect(1) {
      if (it.number != 0) {
        storageManager.showCategory(it, storages)
      } else {
        GAlert(
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
    storagesList = storageManager.getStoragesObservableList(storages)
    table.items = storagesList
    table.refresh()
  }
}
