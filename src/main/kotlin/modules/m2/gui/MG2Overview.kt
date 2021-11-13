package modules.m2.gui

import interfaces.IOverview
import io.ktor.util.*
import javafx.geometry.Orientation
import javafx.scene.paint.Color
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.logic.M2Controller
import modules.mx.isClientGlobal
import modules.mx.rightButtonsWidth
import styling.Stylesheet
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class MG2Overview : IOverview, View("M2 Contacts") {
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
                action { runBlocking { m2Controller.saveEntry() } }
                tooltip("Saves the current contact.")
                prefWidth = rightButtonsWidth
            }
            button("Analytics") {
                isDisable = isClientGlobal
                action { m2Controller.openAnalytics() }
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
                isDisable = isClientGlobal
                action { m2Controller.openDataImport() }
                tooltip("Import contact data from a .csv file.")
                prefWidth = rightButtonsWidth
            }
            // ##################################################
            // ################### Actions ######################
            // ##################################################
            separator(Orientation.HORIZONTAL) {
                paddingVertical = 15
            }
            button("Send EMail") {
                action { m2Controller.showEMailer() }
                tooltip("Opens the EMailer.")
                prefWidth = rightButtonsWidth
            }
        }
        center = form {
            m2Controller.newEntry()
            vbox {
                hbox(10) {
                    style {
                        unsafe("-fx-background-color", Color.web("#373e43", 1.0))
                        paddingAll = 10
                    }
                    addClass(Stylesheet.fieldsetBorder)
                    fieldset("Main Data") {
                        add(NewContactMainData::class)
                        addClass(Stylesheet.fieldsetBorder)
                    }
                    fieldset("Financial Data") {
                        add(NewContactFinancialData::class)
                        addClass(Stylesheet.fieldsetBorder)
                    }
                    fieldset("Profession Data") {
                        add(NewContactProfessionData::class)
                        addClass(Stylesheet.fieldsetBorder)
                    }
                    fieldset("Misc Data") {
                        add(NewContactMiscData::class)
                        addClass(Stylesheet.fieldsetBorder)
                    }
                }
            }
        }
    }
}
