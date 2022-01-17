package modules.m4.gui

import io.ktor.util.*
import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.ItemPriceCategory
import modules.m4.logic.ItemController
import modules.m4.misc.ItemModel
import modules.m4storage.ItemStorage
import modules.m4storage.ItemStorageUnit
import modules.m4storage.gui.GItemStockAdder
import modules.m4storage.logic.ItemStorageManager
import modules.mx.Statistic
import modules.mx.gui.GImageViewer
import modules.mx.gui.userAlerts.GAlert
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class ItemConfiguratorWizard : Wizard("Add new item") {
  val item: ItemModel by inject()

  init {
    enableStepLinks = true
    showHeader = false
    add(ItemMainData::class)
    add(ItemStatistics::class)
    add(ItemPricesData::class)
    add(ItemStorageData::class)
  }
}

@InternalAPI
@ExperimentalSerializationApi
class ItemMainData : Fragment("Main") {
  private val item: ItemModel by inject()

  //----------------------------------v
  //----------- Main Data ------------|
  //----------------------------------^
  override val root = form {
    prefWidth = 600.0
    fieldset {
      field("UID") {
        label(item.uID)
      }
      field("Description") { textfield(item.description).required() }
      field("Article Nr") { textfield(item.articleNumber).required() }
      field("EAN Code") { textfield(item.ean).required() }
      field("Manufacturer Nr") { textfield(item.manufacturerNr).required() }
      field("Image Path") {
        textfield(item.imagePath) {
          isEditable = false
        }
      }
      hbox {
        button("Choose file") {
          action {
            item.imagePath.value = chooseFile(
              "Choose file",
              arrayOf(FileChooser.ExtensionFilter("PNG file (*.png)", "*.png")),
              mode = FileChooserMode.Single
            )[0].path
          }
        }
        button("View image") {
          action {
            if (item.imagePath.value != "?") {
              GImageViewer(Image("file:///${item.imagePath.value}")).openModal()
            } else {
              GAlert("No image loaded.").openModal()
            }
          }
        }
      }
    }
  }

  override fun onSave() {
    isComplete = item.commit()
  }
}

@ExperimentalSerializationApi
@InternalAPI
class ItemStatistics : Fragment("Statistics") {
  private val item: ItemModel by inject()
  private var table = tableview(item.statistics) {
    isEditable = true
    column("Description", Statistic::description) {
      prefWidth = 250.0
      makeEditable()
    }
    column("Value", Statistic::sValue) {
      prefWidth = 250.0
      makeEditable()
    }
    enableCellEditing()
    regainFocusAfterEdit()
    isFocusTraversable = false
  }

  //----------------------------------v
  //----------- Main Data ------------|
  //----------------------------------^
  override val root = form {
    fieldset {
      item.uID.addListener { _, _, _ ->
        table.refresh()
      }
      add(table)
      hbox {
        button("Add Statistic") {
          action {
            item.statistics.value.add(
              Statistic("<Description>", "", 0.0F, false)
            )
            table.refresh()
          }
        }
        button("Remove Statistic") {
          action {
            item.statistics.value.remove(table.selectedItem)
          }
          tooltip("Removes the selected statistic from the item.")
          style { unsafe("-fx-base", Color.DARKRED) }
        }
      }
    }
  }

  override fun onSave() {
    isComplete = item.commit()
  }
}

@InternalAPI
@ExperimentalSerializationApi
class ItemPricesData : Fragment("Prices") {
  private val item: ItemModel by inject()
  private var table = tableview(item.priceCategories) {
    isEditable = true
    readonlyColumn("Number", ItemPriceCategory::number).prefWidth = 100.0
    readonlyColumn("Description", ItemPriceCategory::description).prefWidth = 250.0
    column("Price", ItemPriceCategory::grossPrice) {
      prefWidth = 100.0
      makeEditable()
    }
    enableCellEditing()
    regainFocusAfterEdit()
    isFocusTraversable = false
  }

  //----------------------------------v
  //----------- Main Data ------------|
  //----------------------------------^
  override val root = form {
    fieldset {
      item.uID.addListener { _, _, _ ->
        table.refresh()
      }
      add(table)
    }
  }

  override fun onSave() {
    isComplete = item.commit()
  }
}

@InternalAPI
@ExperimentalSerializationApi
class ItemStorageData : Fragment("Stock") {
  private val item: ItemModel by inject()
  private val lastItemUID = SimpleIntegerProperty(item.uID.value)
  private val storageManager: ItemStorageManager by inject()
  private var selectedStorage: ItemStorage = ItemStorage(0, "")
  private var storagesTable = tableview(item.storages) {
    isEditable = true
    readonlyColumn("#", ItemStorage::number)
    readonlyColumn("Description", ItemStorage::description).remainingWidth()
    readonlyColumn("Lock", ItemStorage::locked) {
      cellFormat {
        text = ""
        style {
          backgroundColor = storageManager.getLockedCellColor(it)
        }
      }
    }
    onUserSelect {
      storageUnitsList.clear()
      selectedStorage = it
      val storage = ItemStorageManager().getStorages().storages[it.number]!!
      for (sUnit in it.storageUnits) {
        storage.storageUnits[sUnit.number].stock = sUnit.stock
        storage.storageUnits[sUnit.number].locked = sUnit.locked
      }
      for (sUnit in storage.storageUnits) storageUnitsList.add(sUnit)
      storageUnitsTable.refresh()
      storageUnitsTable.requestResize()
    }
    enableCellEditing()
    regainFocusAfterEdit()
    columnResizePolicy = SmartResize.POLICY
    isFocusTraversable = false
  }

  var storageUnitsList = observableListOf<ItemStorageUnit>()
  private var storageUnitsTable = tableview(storageUnitsList) {
    readonlyColumn("#", ItemStorageUnit::number)
    readonlyColumn("Description", ItemStorageUnit::description).remainingWidth()
    readonlyColumn("Stock", ItemStorageUnit::stock)
    column("Add", ItemStorageUnit::locked).cellFormat {
      graphic = hbox {
        button("+").action {
          val stockAdder = find<GItemStockAdder>()
          stockAdder.getStorageData(
            storage = selectedStorage,
            storageUnit = rowItem
          )
          stockAdder.openModal(block = true)
          if (!stockAdder.valid()) {
            GAlert(
              "No stock has been added."
            ).openModal()
          } else {
            //Add stock to the row item (visually only)
            rowItem.stock += stockAdder.stockToAddAmount.value
            //Add stock to the underlying item
            ItemController().addStock(
              storageUID = this@ItemStorageData.selectedStorage.number,
              storageUnitUID = rowItem.number,
              amount = stockAdder.stockToAddAmount.value,
              note = stockAdder.note.value
            )
            refresh()
            requestLayout()
          }
        }
      }
    }
    column("Lock", ItemStorageUnit::locked) {
      makeEditable()
    }
    columnResizePolicy = SmartResize.POLICY
    isFocusTraversable = false
  }

  //----------------------------------v
  //----------- Main Data ------------|
  //----------------------------------^
  override val root = borderpane {
    item.uID.addListener { _, _, _ ->
      if (item.uID.value == -1 || item.uID.value != lastItemUID.value) {
        storagesTable.refresh()
        storageUnitsList.clear()
        storageUnitsTable.refresh()
        lastItemUID.value = item.uID.value
      }
    }
    left = form {
      add(storagesTable)
      useMaxWidth = true
      minWidth = 500.0
    }
    center = form {
      add(storageUnitsTable)
    }
  }

  override fun onSave() {
    isComplete = item.commit()
  }
}
