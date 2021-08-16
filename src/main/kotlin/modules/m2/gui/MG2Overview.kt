package modules.m2.gui

import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import modules.interfaces.IOverview
import Styling.Stylesheet
import modules.m2.logic.M2Controller
import modules.mx.rightButtonsWidth
import tornadofx.*

@ExperimentalSerializationApi
class MG2Overview : IOverview, View("M2 Contacts")
{
    private val m2Controller: M2Controller by inject()
    override val root = borderpane {
        right = vbox {
            button("Search") {
                action { m2Controller.searchEntry() }
                tooltip("Opens the search screen.")
                prefWidth = rightButtonsWidth
            }
            button("New") {
                action { m2Controller.newEntry() }
                tooltip("Add a new contact to the database.")
                prefWidth = rightButtonsWidth
            }
            button("Save") {
                action { m2Controller.saveEntry() }
                tooltip("Saves the current contact.")
                prefWidth = rightButtonsWidth
            }
            //Analytics functions
            button("Analytics") {
                action { m2Controller.openAnalytics() }
                tooltip("Display a chart to show the distribution of genres.")
                prefWidth = rightButtonsWidth
            }
            //Maintenance functions
            button("Rebuild indices") {
                //TODO: Not yet implemented
                isDisable = true
                tooltip("Rebuilds all indices in case of faulty indices.")
                prefWidth = rightButtonsWidth
            }
            //Data import
            button("Data Import") {
                action { m2Controller.openDataImport() }
                tooltip("Import contact data from a .csv file.")
                prefWidth = rightButtonsWidth
            }
        }
        center = form {
            vbox {
                hbox(10) {
                    style {
                        unsafe("-fx-background-color", Color.web("#373e43", 1.0))
                        paddingAll = 10
                    }
                    fieldset("Main Data") {
                        add(NewContactMainData::class)
                        addClass(Stylesheet.fieldsetBorder)
                    }
                }
            }
        }
    }
}