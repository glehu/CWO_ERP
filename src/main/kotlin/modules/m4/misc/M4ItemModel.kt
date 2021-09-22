package modules.m4.misc

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.M4Item
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.setValue

@ExperimentalSerializationApi
class M4ItemProperty {
    val uIDProperty = SimpleIntegerProperty(-1)
    var uID: Int by uIDProperty
    val descriptionProperty = SimpleStringProperty()
    var description: String by descriptionProperty
}

@ExperimentalSerializationApi
class M4ItemModel : ItemViewModel<M4ItemProperty>(M4ItemProperty()) {
    val uID = bind(M4ItemProperty::uIDProperty)
    var description = bind(M4ItemProperty::descriptionProperty)
}

@ExperimentalSerializationApi
fun getM4ItemPropertyFromItem(item: M4Item): M4ItemProperty {
    val itemProperty = M4ItemProperty()
    itemProperty.uID = item.uID
    itemProperty.description = item.description
    return itemProperty
}

@ExperimentalSerializationApi
fun getM4ItemFromItemProperty(itemProperty: M4ItemProperty): M4Item {
    val item = M4Item(-1, "")
    item.uID = itemProperty.uID
    item.description = itemProperty.description

    return item
}