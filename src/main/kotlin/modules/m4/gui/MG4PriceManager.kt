package modules.m4.gui

import io.ktor.util.*
import javafx.collections.ObservableList
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.M4PriceCategory
import modules.m4.logic.M4PriceManager
import modules.mx.gui.userAlerts.MGXUserAlert
import modules.mx.logic.MXLog
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
                if (it.number != 0) {
                    categoryManager.showCategory(it, categories, priceCategories)
                } else {
                    MGXUserAlert(
                        MXLog.LogType.INFO,
                        "The default price category cannot be edited.\n\n" +
                                "Please add a new category or edit others, if available."
                    ).openModal()
                }
            }
            isFocusTraversable = false
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

    fun refreshCategories() {
        priceCategories = categoryManager.getCategories(priceCategories, categoryManager.getCategories())
    }
}
