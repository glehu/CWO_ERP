package modules.m3.misc

import io.ktor.util.*
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m3.InvoicePosition
import modules.m3.logic.InvoiceCLIController
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
  val storageFromUIDProperty = SimpleIntegerProperty(-1)
  var storageFromUID by storageFromUIDProperty
  val storageUnitFromUIDProperty = SimpleIntegerProperty(-1)
  var storageUnitFromUID by storageUnitFromUIDProperty

}

class InvoiceItemModel : ItemViewModel<InvoiceItemProperty>() {
  val description = bind(InvoiceItemProperty::descriptionProperty)
  val uID = bind(InvoiceItemProperty::uIDProperty)
  val price = bind(InvoiceItemProperty::priceProperty)
  val amount = bind(InvoiceItemProperty::amountProperty)
  val storageFromUID = bind(InvoiceItemProperty::storageFromUID)
  val storageUnitFromUID = bind(InvoiceItemProperty::storageUnitFromUID)
}

@ExperimentalSerializationApi
fun getItemPropertyFromItem(item: InvoicePosition): InvoiceItemProperty {
  val itemProperty = InvoiceItemProperty()
  itemProperty.uID = item.uID
  itemProperty.description = item.description
  itemProperty.price = item.grossPrice
  itemProperty.amount = item.amount
  itemProperty.storageFromUID = item.storageFromUID
  itemProperty.storageUnitFromUID = item.storageUnitFromUID
  return itemProperty
}

@InternalAPI
@ExperimentalSerializationApi
fun getItemFromItemProperty(itemProperty: InvoiceItemProperty): InvoicePosition {
  val item = InvoicePosition(-1, "")
  item.uID = itemProperty.uID
  item.description = itemProperty.description
  item.grossPrice = itemProperty.price
  item.netPrice = InvoiceCLIController().getNetFromGross(item.grossPrice, 0.19)
  item.amount = itemProperty.amount
  item.storageFromUID = itemProperty.storageFromUID
  item.storageFromUID = itemProperty.storageUnitFromUID
  return item
}
