package modules.m3.misc

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m3.InvoicePosition
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.setValue

class InvoiceItemProperty {
    val descriptionProperty = SimpleStringProperty()
    var description: String by descriptionProperty
    val uIDProperty = SimpleIntegerProperty(-1)
    var uID by uIDProperty
    val priceProperty = SimpleDoubleProperty(0.0)
    var price by priceProperty
    val amountProperty = SimpleDoubleProperty(1.0)
    var amount by amountProperty
    val userNameProperty = SimpleStringProperty("")
    var userName: String by userNameProperty

}

class InvoiceItemModel : ItemViewModel<InvoiceItemProperty>() {
    val description = bind(InvoiceItemProperty::descriptionProperty)
    val uID = bind(InvoiceItemProperty::uIDProperty)
    val price = bind(InvoiceItemProperty::priceProperty)
    val amount = bind(InvoiceItemProperty::amountProperty)
    val userName = bind(InvoiceItemProperty::userNameProperty)
}

@ExperimentalSerializationApi
fun getItemPropertyFromItem(item: InvoicePosition): InvoiceItemProperty {
    val itemProperty = InvoiceItemProperty()
    itemProperty.description = item.description
    itemProperty.price = item.grossPrice
    itemProperty.amount = item.amount
    itemProperty.userName = item.userName
    itemProperty.uID = item.uID
    return itemProperty
}

@ExperimentalSerializationApi
fun getItemFromItemProperty(itemProperty: InvoiceItemProperty): InvoicePosition {
    val item = InvoicePosition(-1, "")
    item.description = itemProperty.description
    item.grossPrice = itemProperty.price
    item.amount = itemProperty.amount
    item.userName = itemProperty.userName
    item.uID = itemProperty.uID
    return item
}
