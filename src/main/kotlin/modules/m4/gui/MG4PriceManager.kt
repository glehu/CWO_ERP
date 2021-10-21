package modules.m4.gui

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.M4PriceCategory
import modules.m4.logic.M4PriceManager
import modules.mx.gui.userAlerts.MGXUserAlert
import modules.mx.rightButtonsWidth
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class MG4PriceManager : View("M4 Price Categories") {
    private val categoryManager: M4PriceManager by inject()

    @ExperimentalSerializationApi
    private val categories = categoryManager.getCategories()
    private var priceCategories = observableListOf<M4PriceCategory>()
    private val table = tableview(priceCategories) {
        readonlyColumn("Number", M4PriceCategory::number).prefWidth(100.0)
        readonlyColumn("Description", M4PriceCategory::description).prefWidth(400.0)
        readonlyColumn("VAT%", M4PriceCategory::vatPercent).prefWidth(75.0)
        onUserSelect(1) {
            if (it.number != 0) {
                categoryManager.showCategory(it, categories)
            } else {
                MGXUserAlert(
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
        priceCategories = categoryManager.getCategories(categoryManager.getCategories())
        table.items = priceCategories
        table.refresh()
    }
}
