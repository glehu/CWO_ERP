package modules.m3.misc

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m3.M3InvoicePosition
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.setValue

class M3ItemProperty {
    val descriptionProperty = SimpleStringProperty()
    var description: String by descriptionProperty
    val uIDProperty = SimpleIntegerProperty(-1)
    var uID by uIDProperty
    val priceProperty = SimpleDoubleProperty(0.0)
    var price by priceProperty
    val amountProperty = SimpleIntegerProperty(1)
    var amount by amountProperty
    val userNameProperty = SimpleStringProperty("")
    var userName: String by userNameProperty

}

class M3ItemModel : ItemViewModel<M3ItemProperty>() {
    val description = bind(M3ItemProperty::descriptionProperty)
    val uID = bind(M3ItemProperty::uIDProperty)
    val price = bind(M3ItemProperty::priceProperty)
    val amount = bind(M3ItemProperty::amountProperty)
    val userName = bind(M3ItemProperty::userNameProperty)
}

@ExperimentalSerializationApi
fun getItemPropertyFromItem(item: M3InvoicePosition): M3ItemProperty {
    val itemProperty = M3ItemProperty()
    itemProperty.description = item.description
    itemProperty.price = item.price
    itemProperty.amount = item.amount
    itemProperty.userName = item.userName
    itemProperty.uID = item.uID
    return itemProperty
}

@ExperimentalSerializationApi
fun getItemFromItemProperty(itemProperty: M3ItemProperty): M3InvoicePosition {
    val item = M3InvoicePosition(-1, "")
    item.description = itemProperty.description
    item.price = itemProperty.price
    item.amount = itemProperty.amount
    item.userName = itemProperty.userName
    item.uID = itemProperty.uID
    return item
}