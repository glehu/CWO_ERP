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
  val storageAmount1Property = SimpleDoubleProperty(0.0)
  var storageAmount1 by storageAmount1Property
  val storageFrom1UIDProperty = SimpleIntegerProperty(-1)
  var storageFrom1UID by storageFrom1UIDProperty
  val storageUnitFrom1UIDProperty = SimpleIntegerProperty(-1)
  var storageUnitFrom1UID by storageUnitFrom1UIDProperty
  val stockPostingsFrom1UIDProperty = mutableMapOf<Int, Double>()
  val storageAmount2Property = SimpleDoubleProperty(0.0)
  var storageAmount2 by storageAmount2Property
  val storageFrom2UIDProperty = SimpleIntegerProperty(-1)
  var storageFrom2UID by storageFrom2UIDProperty
  val storageUnitFrom2UIDProperty = SimpleIntegerProperty(-1)
  var storageUnitFrom2UID by storageUnitFrom2UIDProperty
  val stockPostingsFrom2UIDProperty = mutableMapOf<Int, Double>()
  val storageAmount3Property = SimpleDoubleProperty(0.0)
  var storageAmount3 by storageAmount3Property
  val storageFrom3UIDProperty = SimpleIntegerProperty(-1)
  var storageFrom3UID by storageFrom3UIDProperty
  val storageUnitFrom3UIDProperty = SimpleIntegerProperty(-1)
  var storageUnitFrom3UID by storageUnitFrom3UIDProperty
  val stockPostingsFrom3UIDProperty = mutableMapOf<Int, Double>()
}

class InvoiceItemModel : ItemViewModel<InvoiceItemProperty>() {
  val description = bind(InvoiceItemProperty::descriptionProperty)
  val uID = bind(InvoiceItemProperty::uIDProperty)
  val price = bind(InvoiceItemProperty::priceProperty)
  val amount = bind(InvoiceItemProperty::amountProperty)
  val storageFrom1UID = bind(InvoiceItemProperty::storageFrom1UID)
  val storageUnitFrom1UID = bind(InvoiceItemProperty::storageUnitFrom1UID)
  val stockPostingsFrom1UID = bind(InvoiceItemProperty::stockPostingsFrom1UIDProperty)
  val storageFrom2UID = bind(InvoiceItemProperty::storageFrom2UID)
  val storageUnitFrom2UID = bind(InvoiceItemProperty::storageUnitFrom2UID)
  val stockPostingsFrom2UID = bind(InvoiceItemProperty::stockPostingsFrom2UIDProperty)
  val storageFrom3UID = bind(InvoiceItemProperty::storageFrom3UID)
  val storageUnitFrom3UID = bind(InvoiceItemProperty::storageUnitFrom3UID)
  val stockPostingsFrom3UID = bind(InvoiceItemProperty::stockPostingsFrom3UIDProperty)
}

@ExperimentalSerializationApi
fun getItemPropertyFromItem(item: InvoicePosition): InvoiceItemProperty {
  val itemProperty = InvoiceItemProperty()
  itemProperty.uID = item.uID
  itemProperty.description = item.description
  itemProperty.price = item.grossPrice
  itemProperty.amount = item.amount
  itemProperty.storageFrom1UID = item.storageFrom1UID
  itemProperty.storageUnitFrom1UID = item.storageUnitFrom1UID
  itemProperty.stockPostingsFrom1UIDProperty.clear()
  for (stockPosting in item.stockPostingsFrom1UID) {
    itemProperty.stockPostingsFrom1UIDProperty[stockPosting.key] = stockPosting.value
  }
  itemProperty.storageFrom2UID = item.storageFrom2UID
  itemProperty.storageUnitFrom2UID = item.storageUnitFrom2UID
  itemProperty.stockPostingsFrom2UIDProperty.clear()
  for (stockPosting in item.stockPostingsFrom2UID) {
    itemProperty.stockPostingsFrom2UIDProperty[stockPosting.key] = stockPosting.value
  }
  itemProperty.storageFrom3UID = item.storageFrom3UID
  itemProperty.storageUnitFrom3UID = item.storageUnitFrom3UID
  itemProperty.stockPostingsFrom3UIDProperty.clear()
  for (stockPosting in item.stockPostingsFrom3UID) {
    itemProperty.stockPostingsFrom3UIDProperty[stockPosting.key] = stockPosting.value
  }
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
  item.storageFrom1UID = itemProperty.storageFrom1UID
  item.storageUnitFrom1UID = itemProperty.storageUnitFrom1UID
  item.stockPostingsFrom1UID.clear()
  for (stockPosting in itemProperty.stockPostingsFrom1UIDProperty) {
    item.stockPostingsFrom1UID[stockPosting.key] = stockPosting.value
  }
  item.storageFrom2UID = itemProperty.storageFrom2UID
  item.storageUnitFrom2UID = itemProperty.storageUnitFrom2UID
  item.stockPostingsFrom2UID.clear()
  for (stockPosting in itemProperty.stockPostingsFrom2UIDProperty) {
    item.stockPostingsFrom2UID[stockPosting.key] = stockPosting.value
  }
  item.storageFrom3UID = itemProperty.storageFrom3UID
  item.storageUnitFrom3UID = itemProperty.storageUnitFrom3UID
  item.stockPostingsFrom3UID.clear()
  for (stockPosting in itemProperty.stockPostingsFrom3UIDProperty) {
    item.stockPostingsFrom3UID[stockPosting.key] = stockPosting.value
  }
  return item
}
