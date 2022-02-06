package modules.m3.gui

import interfaces.IOverview
import io.ktor.util.*
import javafx.geometry.Orientation
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m3.logic.InvoiceController
import modules.mx.rightButtonsWidth
import styling.Stylesheet
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class GInvoiceOverview : IOverview, View("Invoices") {
  private val invoiceController: InvoiceController by inject()
  override val root = borderpane {
    right = vbox {
      button("Search") {
        action { invoiceController.searchEntry() }
        tooltip("Opens the search screen.")
        prefWidth = rightButtonsWidth
      }
      button("New") {
        action { invoiceController.newEntry() }
        tooltip("Add a new song to the database.")
        prefWidth = rightButtonsWidth
      }
      button("Save") {
        action { runBlocking { invoiceController.saveEntry() } }
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
        action { runBlocking { invoiceController.showSettings() } }
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
        action { invoiceController.showToDoInvoices() }
        tooltip("Shows invoices that need to be processed.")
        prefWidth = rightButtonsWidth
      }
      // ##################################################
      // ############### Invoice Operations ###############
      // ##################################################
      separator(Orientation.HORIZONTAL) {
        paddingVertical = 15
      }
      button("Commission") {
        action { runBlocking { invoiceController.commissionInvoice() } }
        tooltip("Commissions the invoice.")
        prefWidth = rightButtonsWidth
        style { unsafe("-fx-base", Color.DARKGREEN) }
      }
      button("Payment") {
        action { runBlocking { invoiceController.payInvoice() } }
        tooltip("Opens up the payment screen.")
        prefWidth = rightButtonsWidth
        style { unsafe("-fx-base", Color.DARKGREEN) }
      }
      button("Process") {
        action { runBlocking { invoiceController.processInvoice() } }
        tooltip("Processes the invoice and finalizes it.")
        prefWidth = rightButtonsWidth
        style { unsafe("-fx-base", Color.DARKGREEN) }
      }
      button("Cancel") {
        action { runBlocking { invoiceController.cancelInvoice() } }
        tooltip("Cancels the invoice.")
        prefWidth = rightButtonsWidth
        style { unsafe("-fx-base", Color.DARKRED) }
      }
    }
    center = form {
      invoiceController.newEntry()
      vbox {
        hbox(10) {
          style {
            unsafe("-fx-background-color", Color.web("#373e43", 1.0))
            paddingAll = 10
          }
          addClass(Stylesheet.fieldsetBorder)
          fieldset("Main Data") {
            add(InvoiceMainData::class)
            addClass(Stylesheet.fieldsetBorder)
          }
          fieldset {
            add(InvoiceNotesData::class)
            addClass(Stylesheet.fieldsetBorder)
            hgrow = Priority.ALWAYS
          }
        }
        fieldset {
          add(InvoiceItemData::class)
          addClass(Stylesheet.fieldsetBorder)
          hgrow = Priority.ALWAYS
        }
      }
    }
  }
}
