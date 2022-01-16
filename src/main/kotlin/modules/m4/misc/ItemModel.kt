package modules.m4.misc

import io.ktor.util.*
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m4.Item
import modules.m4.ItemPriceCategory
import modules.m4.logic.ItemPriceManager
import modules.m4storage.ItemStorage
import modules.m4storage.ItemStorageUnit
import modules.m4storage.logic.ItemStorageManager
import modules.mx.Statistic
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.observableListOf
import tornadofx.setValue

@InternalAPI
@ExperimentalSerializationApi
class ItemProperty {
  val uIDProperty = SimpleIntegerProperty(-1)
  var uID: Int by uIDProperty
  val descriptionProperty = SimpleStringProperty()
  var description: String by descriptionProperty
  val articleNumberProperty = SimpleStringProperty("?")
  var articleNumber: String by articleNumberProperty
  val eanProperty = SimpleStringProperty("?")
  var ean: String by eanProperty
  val manufacturerCodeProperty = SimpleStringProperty("?")
  var manufacturerCode: String by manufacturerCodeProperty
  val imagePathProperty = SimpleStringProperty("?")
  var imagePath: String by imagePathProperty
  val productInfoJsonProperty = SimpleStringProperty("?")
  var productInfoJson: String by productInfoJsonProperty
  var statisticsProperty = observableListOf<Statistic>()
  var priceCategoriesProperty = ItemPriceManager().getCategories(ItemPriceManager().getCategories())
  var storagesProperty = ItemStorageManager().getStoragesObservableList(ItemStorageManager().getStorages())
}

@InternalAPI
@ExperimentalSerializationApi
class ItemModel : ItemViewModel<ItemProperty>() {
  val uID = bind(ItemProperty::uIDProperty)
  val description = bind(ItemProperty::descriptionProperty)
  val articleNumber = bind(ItemProperty::articleNumberProperty)
  val ean = bind(ItemProperty::eanProperty)
  val manufacturerNr = bind(ItemProperty::manufacturerCodeProperty)
  val imagePath = bind(ItemProperty::imagePathProperty)
  val statistics = bind(ItemProperty::statisticsProperty)
  val priceCategories = bind(ItemProperty::priceCategoriesProperty)
  val storages = bind(ItemProperty::storagesProperty)
}

@InternalAPI
@ExperimentalSerializationApi
fun getItemPropertyFromItem(item: Item): ItemProperty {
  val itemProperty = ItemProperty()
  itemProperty.uID = item.uID
  itemProperty.description = item.description
  itemProperty.articleNumber = item.articleNumber
  itemProperty.ean = item.ean
  itemProperty.manufacturerCode = item.manufacturerCode
  itemProperty.imagePath = item.imagePath
  itemProperty.productInfoJson = item.productInfoJson
  // Get the current price categories, so we are working with the latest data
  itemProperty.priceCategoriesProperty = ItemPriceManager().getCategories(ItemPriceManager().getCategories())
  // Fill the price categories' prices according to their number
  for ((_, priceCategoryString) in item.prices) {
    val priceCategory = Json.decodeFromString<ItemPriceCategory>(priceCategoryString)
    itemProperty.priceCategoriesProperty[priceCategory.number].grossPrice = priceCategory.grossPrice
  }
  // Get the current storage locations, so we are working with the latest data
  itemProperty.storagesProperty = ItemStorageManager().getStoragesObservableList(ItemStorageManager().getStorages())
  // Fill the storage locations' stock amount according to their number
  for ((_, storageString) in item.stock) {
    val storage = Json.decodeFromString<ItemStorage>(storageString)
    for (storageUnit in storage.storageUnits) {
      itemProperty.storagesProperty[storage.number].storageUnits[storageUnit.number].stock = storageUnit.stock
      itemProperty.storagesProperty[storage.number].storageUnits[storageUnit.number].locked = storageUnit.locked
    }
  }
  // Fill the item's statistics
  for ((_, statisticString) in item.statistics) {
    itemProperty.statisticsProperty.add(Json.decodeFromString<Statistic>(statisticString))
  }
  return itemProperty
}

@InternalAPI
@ExperimentalSerializationApi
fun getItemFromItemProperty(itemProperty: ItemProperty): Item {
  val item = Item(-1, "")
  item.uID = itemProperty.uID
  item.description = itemProperty.description
  item.articleNumber = itemProperty.articleNumber
  item.ean = itemProperty.ean
  item.manufacturerCode = itemProperty.manufacturerCode
  item.imagePath = itemProperty.imagePath
  item.productInfoJson = itemProperty.productInfoJson
  for (statistic in itemProperty.statisticsProperty) {
    item.statistics[statistic.description] = Json.encodeToString(statistic)
  }
  for (price in itemProperty.priceCategoriesProperty) {
    item.prices[item.prices.size] = Json.encodeToString(price)
  }
  for (storage in itemProperty.storagesProperty) {
    val storageUnits = mutableListOf<ItemStorageUnit>()
    for (storageUnit in storage.storageUnits) {
      if (storageUnit.stock != 0.0) storageUnits.add(storageUnit)
    }
    storage.storageUnits = storageUnits
    if (storage.storageUnits.isNotEmpty()) item.stock[item.stock.size] = Json.encodeToString(storage)
  }
  return item
}
