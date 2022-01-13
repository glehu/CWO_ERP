package modules.m4storage.gui

import io.ktor.util.*
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4storage.ItemStorage
import modules.m4storage.ItemStorageUnit
import modules.m4storage.logic.ItemStorageManager
import modules.m4storage.misc.ItemStorageModel
import modules.m4storage.misc.getStorageFromStorageProperty
import modules.m4storage.misc.getStoragePropertyFromStorage
import modules.mx.rightButtonsWidth
import tornadofx.*

@ExperimentalSerializationApi
@InternalAPI
class GItemStorage(storage: ItemStorage) : Fragment("Storage Locations") {
  private val storageManager: ItemStorageManager by inject()
  private val storageModel = ItemStorageModel(getStoragePropertyFromStorage(storage))
  private val originalStorageProperty = storage.copy()
  private val storageUnitsTable = tableview(storageModel.storageUnits) {
    isEditable = true
    readonlyColumn("#", ItemStorageUnit::number)
    column("Description", ItemStorageUnit::description) {
      makeEditable()
      prefWidth(200.0)
    }
    column("Lock", ItemStorageUnit::locked) {
      makeEditable()
    }
    enableCellEditing()
    regainFocusAfterEdit()
    isFocusTraversable = false
  }
  override val root = form {
    fieldset {
      field("Number") { label(storageModel.number) }
      field("Description") { textfield(storageModel.description).required() }
      field("Locked") {
        checkbox("", storageModel.locked) {
          tooltip("If checked, locks all items' stock matched to this location from being sold.")
        }
      }
      field("Storage Units") {
        add(storageUnitsTable)
        vbox {
          button("Add Unit") {
            action {
              storageModel.storageUnits.value.add(
                ItemStorageUnit(storageModel.storageUnits.value.size, "?")
              )
              storageUnitsTable.refresh()
            }
            prefWidth = rightButtonsWidth
          }
          button("Remove Unit") {
            action {
              storageModel.storageUnits.value.remove(storageUnitsTable.selectedItem)
            }
            tooltip("Removes the selected statistic from the item.")
            style { unsafe("-fx-base", Color.DARKRED) }
            prefWidth = rightButtonsWidth
          }
        }
      }
    }
    button("Save (CTRL+S)") {
      shortcut("CTRL+S")
      action {
        storageModel.validate()
        if (storageModel.isValid) {
          storageModel.commit()
          storageManager.funUpdateStorage(
            storageNew = getStorageFromStorageProperty(storageModel.item),
            storageOld = originalStorageProperty
          )
          close()
        }
      }
      prefWidth = rightButtonsWidth
    }
    button("Delete") {
      prefWidth = rightButtonsWidth
      action {
        storageManager.deleteStorage(originalStorageProperty)
        close()
      }
      style { unsafe("-fx-base", Color.DARKRED) }
      vboxConstraints { marginTop = 25.0 }
    }
  }
}
