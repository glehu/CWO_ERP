package modules.m4.misc

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import modules.m4.M4Storage
import modules.m4.M4StorageUnit
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.observableListOf
import tornadofx.setValue

class M4StorageProperty {
    //Credentials
    val numberProperty = SimpleIntegerProperty(-1)
    var number: Int by numberProperty
    val descriptionProperty = SimpleStringProperty("")
    var description: String by descriptionProperty
    val lockedProperty = SimpleBooleanProperty(false)
    var locked: Boolean by lockedProperty
    val storageUnitsProperty = observableListOf<M4StorageUnit>()
}

class M4StorageModel(category: M4StorageProperty) : ItemViewModel<M4StorageProperty>(category) {
    val number = bind(M4StorageProperty::numberProperty)
    val description = bind(M4StorageProperty::descriptionProperty)
    val locked = bind(M4StorageProperty::lockedProperty)
    val storageUnits = bind(M4StorageProperty::storageUnitsProperty)
}

fun getStoragePropertyFromStorage(storage: M4Storage): M4StorageProperty {
    val storageProperty = M4StorageProperty()
    storageProperty.number = storage.number
    storageProperty.description = storage.description
    storageProperty.locked = storage.locked
    for (storageUnit in storage.storageUnits) {
        storageProperty.storageUnitsProperty.add(storageUnit)
    }
    if (storageProperty.storageUnitsProperty.size <= 0) {
        storageProperty.storageUnitsProperty.add(M4StorageUnit(0, ""))
    }
    return storageProperty
}

fun getStorageFromStorageProperty(storageProperty: M4StorageProperty): M4Storage {
    val storage = M4Storage(storageProperty.number, storageProperty.description, storageProperty.locked)
    for (storageUnit in storageProperty.storageUnitsProperty) {
        storage.storageUnits += storageUnit
    }
    return storage
}
