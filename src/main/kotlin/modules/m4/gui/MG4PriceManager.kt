package modules.m4.gui

import io.ktor.util.*
import javafx.collections.ObservableList
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.M4PriceCategory
import modules.m4.logic.M4PriceManager
import modules.mx.rightButtonsWidth
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class MG4PriceManager : View("M4 Price Categories") {
    private val categoryManager: M4PriceManager by inject()

    @ExperimentalSerializationApi
    private val categories = categoryManager.getCategories()
    private var priceCategories: ObservableList<M4PriceCategory> = observableListOf()
    override val root = borderpane {
        priceCategories = categoryManager.getCategories(priceCategories, categories)
        center = tableview(priceCategories) {
            readonlyColumn("Number", M4PriceCategory::number).prefWidth(100.0)
            readonlyColumn("Description", M4PriceCategory::description).prefWidth(400.0)
            onUserSelect(1) {
                categoryManager.showCategory(it, categories, priceCategories)
            }
        }
        right = vbox {
            button("Add category") {
                action {
                    categoryManager.addCategory(categories, priceCategories)
                }
                prefWidth = rightButtonsWidth
            }
        }
    }
}
