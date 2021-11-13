package modules.m4.misc

import io.ktor.util.*
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m4.M4Item
import modules.m4.M4PriceCategory
import modules.m4.Statistic
import modules.m4.logic.M4PriceManager
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.observableListOf
import tornadofx.setValue

@InternalAPI
@ExperimentalSerializationApi
class M4ItemProperty {
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
    var priceCategoriesProperty = M4PriceManager().getCategories(M4PriceManager().getCategories())
}

@InternalAPI
@ExperimentalSerializationApi
class M4ItemModel : ItemViewModel<M4ItemProperty>() {
    val uID = bind(M4ItemProperty::uIDProperty)
    val description = bind(M4ItemProperty::descriptionProperty)
    val articleNumber = bind(M4ItemProperty::articleNumberProperty)
    val ean = bind(M4ItemProperty::eanProperty)
    val manufacturerNr = bind(M4ItemProperty::manufacturerCodeProperty)
    val imagePath = bind(M4ItemProperty::imagePathProperty)
    val productInfoJson = bind(M4ItemProperty::productInfoJsonProperty)
    val statistics = bind(M4ItemProperty::statisticsProperty)
    val priceCategories = bind(M4ItemProperty::priceCategoriesProperty)
}

@InternalAPI
@ExperimentalSerializationApi
fun getM4ItemPropertyFromItem(item: M4Item): M4ItemProperty {
    val itemProperty = M4ItemProperty()
    itemProperty.uID = item.uID
    itemProperty.description = item.description
    itemProperty.articleNumber = item.articleNumber
    itemProperty.ean = item.ean
    itemProperty.manufacturerCode = item.manufacturerCode
    itemProperty.imagePath = item.imagePath
    itemProperty.productInfoJson = item.productInfoJson
    /**
     * Get the current price categories, so we are working with the latest data
     */
    itemProperty.priceCategoriesProperty = M4PriceManager().getCategories(M4PriceManager().getCategories())
    /**
     * Fill the item's statistics
     */
    for ((_, statisticString) in item.statistics) {
        val statistic = Json.decodeFromString<Statistic>(statisticString)
        itemProperty.statisticsProperty.add(statistic)
    }
    /**
     * Fill the price categories' prices according to their number
     */
    for ((_, priceCategoryString) in item.prices) {
        val priceCategory = Json.decodeFromString<M4PriceCategory>(priceCategoryString)
        itemProperty.priceCategoriesProperty[priceCategory.number].grossPrice = priceCategory.grossPrice
    }
    return itemProperty
}

@InternalAPI
@ExperimentalSerializationApi
fun getM4ItemFromItemProperty(itemProperty: M4ItemProperty): M4Item {
    val item = M4Item(-1, "")
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
    return item
}
