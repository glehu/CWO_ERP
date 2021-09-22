package modules.m4.gui

import interfaces.IOverview
import io.ktor.util.*
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.logic.M4Controller
import modules.mx.rightButtonsWidth
import styling.Stylesheet
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class MG4Overview : IOverview, View("M4 Item") {
    private val m4Controller: M4Controller by inject()
    override val root = borderpane {
        right = vbox {
            button("Search") {
                action { m4Controller.searchEntry() }
                tooltip("Opens the search screen.")
                prefWidth = rightButtonsWidth
            }
            button("New") {
                action { m4Controller.newEntry() }
                tooltip("Add a new contact to the database.")
                prefWidth = rightButtonsWidth
            }
            button("Save") {
                action { m4Controller.saveEntry() }
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
            button("Rebuild indices") {
                //TODO: Not yet implemented
                isDisable = true
                tooltip("Rebuilds all indices in case of faulty indices.")
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
            m4Controller.newEntry()
            vbox {
                hbox(10) {
                    style {
                        unsafe("-fx-background-color", Color.web("#373e43", 1.0))
                        paddingAll = 10
                    }
                    fieldset("Main Data") {
                        add(NewM4ItemMainData::class)
                        addClass(Stylesheet.fieldsetBorder)
                    }
                }
            }
        }
    }
}