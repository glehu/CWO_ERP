package modules.m4.gui

import interfaces.IOverview
import io.ktor.util.*
import javafx.scene.paint.Color
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.logic.ItemController
import modules.mx.rightButtonsWidth
import styling.Stylesheet
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class GItemOverview : IOverview, View("M4 Inventory") {
    private val itemController: ItemController by inject()
    override val root = borderpane {
        right = vbox {
            button("Search") {
                action { itemController.searchEntry() }
                tooltip("Opens the search screen.")
                prefWidth = rightButtonsWidth
            }
            button("New") {
                action { itemController.newEntry() }
                tooltip("Add a new contact to the database.")
                prefWidth = rightButtonsWidth
            }
            button("Save") {
                action { runBlocking { itemController.saveEntry() } }
                tooltip("Saves the current contact.")
                prefWidth = rightButtonsWidth
            }
            button("Analytics") {
                //TODO: Not yet implemented
                isDisable = true
                //action { m4Controller.openAnalytics() }
                tooltip("TBD")
                prefWidth = rightButtonsWidth
            }
            button("Settings") {
                action { runBlocking { itemController.showSettings() } }
                tooltip("Opens the settings screen.")
                prefWidth = rightButtonsWidth
            }
            button("Data Import") {
                //TODO: Not yet implemented
                isDisable = true
                //action { m4Controller.openDataImport() }
                tooltip("TBD")
                prefWidth = rightButtonsWidth
            }
        }
        center = form {
            itemController.newEntry()
            vbox {
                hbox(10) {
                    style {
                        unsafe("-fx-background-color", Color.web("#373e43", 1.0))
                        paddingAll = 10
                    }
                    addClass(Stylesheet.fieldsetBorder)
                    fieldset("Main Data") {
                        add(NewM4ItemMainData::class)
                        addClass(Stylesheet.fieldsetBorder)
                    }
                    fieldset("Prices") {
                        add(NewM4ItemPricesData::class)
                        addClass(Stylesheet.fieldsetBorder)
                    }
                    fieldset("Statistics") {
                        add(NewM4ItemStatistics::class)
                        addClass(Stylesheet.fieldsetBorder)
                    }
                }
                fieldset("Stock") {
                    add(NewM4ItemStorageData::class)
                    addClass(Stylesheet.fieldsetBorder)
                }
            }
        }
    }
}
