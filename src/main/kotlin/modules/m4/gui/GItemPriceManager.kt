package modules.m4.gui

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.ItemPriceCategory
import modules.m4.logic.ItemPriceManager
import modules.mx.gui.userAlerts.GAlert
import modules.mx.rightButtonsWidth
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class GItemPriceManager : View("Item Price Categories") {
    private val categoryManager: ItemPriceManager by inject()

    @ExperimentalSerializationApi
    private var categories = categoryManager.getCategories()
    private var priceCategories = observableListOf<ItemPriceCategory>()
    private val table = tableview(priceCategories) {
        readonlyColumn("Number", ItemPriceCategory::number).prefWidth(100.0)
        readonlyColumn("Description", ItemPriceCategory::description).prefWidth(400.0)
        readonlyColumn("VAT%", ItemPriceCategory::vatPercent).prefWidth(75.0)
        onUserSelect(1) {
            if (it.number != 0) {
                categoryManager.showCategory(it, categories)
            } else {
                GAlert(
                    "The default price category cannot be edited.\n\n" +
                            "Please add a new category or edit others, if available."
                ).openModal()
            }
        }
        isFocusTraversable = false
    }
    override val root = borderpane {
        refreshCategories()
        center = table
        right = vbox {
            button("Add category") {
                action {
                    categoryManager.addCategory(categories)
                }
                prefWidth = rightButtonsWidth
            }
        }
    }

    fun refreshCategories() {
        categories = categoryManager.getCategories()
        priceCategories = categoryManager.getCategories(categories)
        table.items = priceCategories
        table.refresh()
    }
}
