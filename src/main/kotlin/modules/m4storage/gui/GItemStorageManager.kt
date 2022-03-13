package modules.m4storage.gui

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4storage.ItemStorage
import modules.m4storage.logic.ItemStorageManager
import modules.mx.gui.userAlerts.GAlert
import modules.mx.rightButtonsWidth
import tornadofx.View
import tornadofx.action
import tornadofx.borderpane
import tornadofx.button
import tornadofx.observableListOf
import tornadofx.onUserSelect
import tornadofx.prefWidth
import tornadofx.readonlyColumn
import tornadofx.style
import tornadofx.tableview
import tornadofx.vbox

@InternalAPI
@ExperimentalSerializationApi
class GItemStorageManager(var isStorageSelectMode: Boolean = false) : View("Item Storage Locations") {
  private val storageManager = ItemStorageManager()

  var selectedStorageUID: Int = -1
  var selectedStorageUnitUID: Int = -1

  @ExperimentalSerializationApi
  private var storages = storageManager.getStorages()
  private var storagesList = observableListOf<ItemStorage>()
  private val table = tableview(storagesList) {
    readonlyColumn("Number", ItemStorage::number).prefWidth(100.0)
    readonlyColumn("Description", ItemStorage::description).prefWidth(400.0)
    readonlyColumn("Lock", ItemStorage::locked).prefWidth(50.0)
      .cellFormat { text = ""; style { backgroundColor = storageManager.getLockedCellColor(it) } }
    onUserSelect(1) {
      if (isStorageSelectMode || it.number != 0) {
        selectedStorageUID = it.number
        selectedStorageUnitUID = if (!isStorageSelectMode || it.storageUnits.size > 1) {
          storageManager.showItemStorageUnit(it, storages, isStorageSelectMode)
        } else {
          0
        }
        if (isStorageSelectMode) close()
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
    if (!isStorageSelectMode) {
      right = vbox {
        button("Add Storage") {
          action {
            storageManager.addStorage(storages)
          }
          prefWidth = rightButtonsWidth
        }
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
