package modules.m3.gui

import interfaces.IOverview
import io.ktor.util.*
import javafx.geometry.Orientation
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import kotlinx.coroutines.runBlocking
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
                action { runBlocking { m3Controller.saveEntry() } }
                tooltip("Saves the current contact.")
                prefWidth = rightButtonsWidth
            }
            button("Analytics") {
                //TODO: Not yet implemented
                isDisable = true
                tooltip("Display a chart to show the distribution of genres.")
                prefWidth = rightButtonsWidth
            }
            button("Settings") {
                action { runBlocking { m3Controller.showSettings() } }
                tooltip("Opens the settings screen.")
                prefWidth = rightButtonsWidth
            }
            button("Data Import") {
                //TODO: Not yet implemented
                isDisable = true
                tooltip("Import contact data from a .csv file.")
                prefWidth = rightButtonsWidth
            }
            // ##################################################
            // ################### Lookups ######################
            // ##################################################
            separator(Orientation.HORIZONTAL) {
                paddingVertical = 15
            }
            button("To Do") {
                action { m3Controller.showToDoInvoices() }
                tooltip("Shows invoices that need to be processed.")
                prefWidth = rightButtonsWidth
            }
            // ##################################################
            // ############### Invoice Operations ###############
            // ##################################################
            separator(Orientation.HORIZONTAL) {
                paddingVertical = 15
            }
            button("Paid") {
                action { runBlocking { m3Controller.setPaidInvoice() } }
                tooltip("Sets the invoice to fully paid.")
                prefWidth = rightButtonsWidth
                style { unsafe("-fx-base", Color.DARKGREEN) }
            }
            button("Process") {
                action { runBlocking { m3Controller.processInvoice() } }
                tooltip("Processes the invoice.")
                prefWidth = rightButtonsWidth
                style { unsafe("-fx-base", Color.DARKGREEN) }
            }
            button("Cancel") {
                action { runBlocking { m3Controller.cancelInvoice() } }
                tooltip("Cancels the invoice.")
                prefWidth = rightButtonsWidth
                style { unsafe("-fx-base", Color.DARKRED) }
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
                    addClass(Stylesheet.fieldsetBorder)
                    fieldset("Main Data") {
                        add(NewInvoiceMainData::class)
                        addClass(Stylesheet.fieldsetBorder)
                    }
                    fieldset {
                        add(NewInvoiceItemData::class)
                        addClass(Stylesheet.fieldsetBorder)
                        hgrow = Priority.ALWAYS
                    }
                }
                fieldset {
                    add(NewInvoiceNotes::class)
                    addClass(Stylesheet.fieldsetBorder)
                    hgrow = Priority.ALWAYS
                }
            }
        }
    }
}
