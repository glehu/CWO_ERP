package modules.m4.misc

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import modules.m4.M4Storage
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.setValue

class M4StorageProperty {
    //Credentials
    val numberProperty = SimpleIntegerProperty(-1)
    var number: Int by numberProperty
    val descriptionProperty = SimpleStringProperty("")
    var description: String by descriptionProperty
}

class M4StorageModel(category: M4StorageProperty) : ItemViewModel<M4StorageProperty>(category) {
    val number = bind(M4StorageProperty::numberProperty)
    val description = bind(M4StorageProperty::descriptionProperty)
}

fun getStoragePropertyFromStorage(storage: M4Storage): M4StorageProperty {
    val storageProperty = M4StorageProperty()
    storageProperty.number = storage.number
    storageProperty.description = storage.description
    return storageProperty
}

fun getStorageFromStorageProperty(storageProperty: M4StorageProperty): M4Storage {
    return M4Storage(storageProperty.number, storageProperty.description)
}
