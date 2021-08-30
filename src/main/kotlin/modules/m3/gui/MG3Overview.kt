package modules.m3.gui

import interfaces.IOverview
import io.ktor.util.*
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m3.logic.M3Controller
import modules.mx.rightButtonsWidth
import styling.Stylesheet
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class MG3Overview : IOverview, View("M3 Invoices") {
    private val m3Controller: M3Controller by inject()
    override val root = borderpane {
        right = vbox {
            button("Search") {
                action { m3Controller.searchEntry() }
                tooltip("Opens the search screen.")
                prefWidth = rightButtonsWidth
            }
            button("New") {
                action { m3Controller.newEntry() }
                tooltip("Add a new song to the database.")
                prefWidth = rightButtonsWidth
            }
            button("Save") {
                action { m3Controller.saveEntry() }
                tooltip("Saves the current contact.")
                prefWidth = rightButtonsWidth
            }
            button("Analytics") {
                //TODO: Not yet implemented
                isDisable = true
                tooltip("Display a chart to show the distribution of genres.")
                prefWidth = rightButtonsWidth
            }
            button("Rebuild indices") {
                //TODO: Not yet implemented
                isDisable = true
                tooltip("Rebuilds all indices in case of faulty indices.")
                prefWidth = rightButtonsWidth
            }
            button("Data Import") {
                //TODO: Not yet implemented
                isDisable = true
                tooltip("Import contact data from a .csv file.")
                prefWidth = rightButtonsWidth
            }
        }
        center = form {
            m3Controller.newEntry()
            vbox {
                hbox(10) {
                    style {
                        unsafe("-fx-background-color", Color.web("#373e43", 1.0))
                        paddingAll = 10
                    }
                    fieldset("Main Data") {
                        add(NewInvoiceMainData::class)
                        addClass(Stylesheet.fieldsetBorder)
                    }
                }
            }
        }
    }
}
