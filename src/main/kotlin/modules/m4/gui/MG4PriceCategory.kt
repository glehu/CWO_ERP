package modules.m4.gui

import io.ktor.util.*
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.M4PriceCategories
import modules.m4.M4PriceCategory
import modules.m4.logic.M4PriceManager
import modules.m4.misc.M4PriceCategoryModel
import modules.m4.misc.getPriceCategoryFromCategoryProperty
import modules.m4.misc.getPriceCategoryPropertyFromCategory
import modules.mx.rightButtonsWidth
import tornadofx.*

@ExperimentalSerializationApi
@InternalAPI
class MG4PriceCategory(
    priceCategory: M4PriceCategory,
    categories: M4PriceCategories
) : Fragment("Price Category") {
    private val priceManager: M4PriceManager by inject()
    private val priceCategoryModel = M4PriceCategoryModel(getPriceCategoryPropertyFromCategory(priceCategory))
    private val originalPriceCategory = priceCategory.copy()
    override val root = form {
        fieldset("Main Data") {
            field("Number") { label(priceCategoryModel.number) }
            field("Description") { textfield(priceCategoryModel.description).required() }
        }
        button("Save") {
            shortcut("Enter")
            action {
                priceCategoryModel.commit()
                priceManager.updateCategory(
                    getPriceCategoryFromCategoryProperty(priceCategoryModel.item), originalPriceCategory, categories
                )
                close()
            }
            prefWidth = rightButtonsWidth
        }
        button("Delete") {
            prefWidth = rightButtonsWidth
            action {
                priceManager.deleteCategory(originalPriceCategory, categories)
                close()
            }
            style { unsafe("-fx-base", Color.DARKRED) }
            vboxConstraints { marginTop = 25.0 }
        }
    }
}