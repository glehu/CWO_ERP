package modules.m4.misc

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import modules.m4.ItemPriceCategory
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.setValue

class ItemPriceCategoryProperty {
  //Credentials
  val numberProperty = SimpleIntegerProperty(-1)
  var number: Int by numberProperty
  val descriptionProperty = SimpleStringProperty("")
  var description: String by descriptionProperty
  val vatPercentProperty = SimpleDoubleProperty(19.0)
  var vatPercent: Double by vatPercentProperty
}

class M4PriceCategoryModel(category: ItemPriceCategoryProperty) : ItemViewModel<ItemPriceCategoryProperty>(category) {
  val number = bind(ItemPriceCategoryProperty::numberProperty)
  val description = bind(ItemPriceCategoryProperty::descriptionProperty)
  val vatPercent = bind(ItemPriceCategoryProperty::vatPercentProperty)
}

fun getPriceCategoryPropertyFromCategory(category: ItemPriceCategory): ItemPriceCategoryProperty {
  val categoryProperty = ItemPriceCategoryProperty()
  categoryProperty.number = category.number
  categoryProperty.description = category.description
  categoryProperty.vatPercent = category.vatPercent
  return categoryProperty
}

fun getPriceCategoryFromCategoryProperty(categoryProperty: ItemPriceCategoryProperty): ItemPriceCategory {
  return ItemPriceCategory(categoryProperty.number, categoryProperty.description, categoryProperty.vatPercent)
}
