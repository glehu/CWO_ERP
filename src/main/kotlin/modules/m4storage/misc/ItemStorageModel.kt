package modules.m4storage.misc

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import modules.m4storage.ItemStorage
import modules.m4storage.ItemStorageUnit
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.observableListOf
import tornadofx.setValue

class ItemStorageProperty {
  //Credentials
  val numberProperty = SimpleIntegerProperty(-1)
  var number: Int by numberProperty
  val descriptionProperty = SimpleStringProperty("")
  var description: String by descriptionProperty
  val lockedProperty = SimpleBooleanProperty(false)
  var locked: Boolean by lockedProperty
  val storageUnitsProperty = observableListOf<ItemStorageUnit>()
}

class ItemStorageModel(category: ItemStorageProperty) : ItemViewModel<ItemStorageProperty>(category) {
  val number = bind(ItemStorageProperty::numberProperty)
  val description = bind(ItemStorageProperty::descriptionProperty)
  val locked = bind(ItemStorageProperty::lockedProperty)
  val storageUnits = bind(ItemStorageProperty::storageUnitsProperty)
}

fun getStoragePropertyFromStorage(storage: ItemStorage): ItemStorageProperty {
  val storageProperty = ItemStorageProperty()
  storageProperty.number = storage.number
  storageProperty.description = storage.description
  storageProperty.locked = storage.locked
  for (storageUnit in storage.storageUnits) {
    storageProperty.storageUnitsProperty.add(storageUnit)
  }
  if (storageProperty.storageUnitsProperty.size <= 0) {
    storageProperty.storageUnitsProperty.add(ItemStorageUnit(0, ""))
  }
  return storageProperty
}

fun getStorageFromStorageProperty(storageProperty: ItemStorageProperty): ItemStorage {
  val storage = ItemStorage(storageProperty.number, storageProperty.description, storageProperty.locked)
  for (storageUnit in storageProperty.storageUnitsProperty) {
    storage.storageUnits += storageUnit
  }
  return storage
}
