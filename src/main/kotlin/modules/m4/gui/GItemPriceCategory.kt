package modules.m4.gui

import io.ktor.util.*
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.ItemPriceCategory
import modules.m4.logic.ItemPriceManager
import modules.m4.misc.M4PriceCategoryModel
import modules.m4.misc.getPriceCategoryFromCategoryProperty
import modules.m4.misc.getPriceCategoryPropertyFromCategory
import modules.mx.rightButtonsWidth
import tornadofx.*

@ExperimentalSerializationApi
@InternalAPI
class GItemPriceCategory(priceCategory: ItemPriceCategory) : Fragment("Price Category") {
  private val priceManager: ItemPriceManager by inject()
  private val priceCategoryModel = M4PriceCategoryModel(getPriceCategoryPropertyFromCategory(priceCategory))
  private val originalPriceCategory = priceCategory.copy()
  override val root = form {
    fieldset {
      field("Number") { label(priceCategoryModel.number) }
      field("Description") { textfield(priceCategoryModel.description).required() }
      field("VAT%") { textfield(priceCategoryModel.vatPercent).required() }
    }
    button("Save") {
      shortcut("Enter")
      action {
        priceCategoryModel.validate()
        if (priceCategoryModel.isValid) {
          priceCategoryModel.commit()
          priceManager.updateCategory(
            categoryNew = getPriceCategoryFromCategoryProperty(priceCategoryModel.item),
            categoryOld = originalPriceCategory
          )
          close()
        }
      }
      prefWidth = rightButtonsWidth
    }
    button("Delete") {
      prefWidth = rightButtonsWidth
      action {
        priceManager.deleteCategory(originalPriceCategory)
        close()
      }
      style { unsafe("-fx-base", Color.DARKRED) }
      vboxConstraints { marginTop = 25.0 }
    }
  }
}
