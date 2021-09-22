package modules.m4.misc

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import modules.m4.M4PriceCategory
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.setValue

class M4PriceCategoryProperty {
    //Credentials
    val numberProperty = SimpleIntegerProperty(-1)
    var number: Int by numberProperty
    val descriptionProperty = SimpleStringProperty("")
    var description: String by descriptionProperty
}

class M4PriceCategoryModel(category: M4PriceCategoryProperty) : ItemViewModel<M4PriceCategoryProperty>(category) {
    val number = bind(M4PriceCategoryProperty::numberProperty)
    val description = bind(M4PriceCategoryProperty::descriptionProperty)
}

fun getPriceCategoryPropertyFromCategory(category: M4PriceCategory): M4PriceCategoryProperty {
    val categoryProperty = M4PriceCategoryProperty()
    categoryProperty.number = category.number
    categoryProperty.description = category.description
    return categoryProperty
}

fun getPriceCategoryFromCategoryProperty(categoryProperty: M4PriceCategoryProperty): M4PriceCategory {
    return M4PriceCategory(categoryProperty.number, categoryProperty.description)
}