package modules.m2.gui

import interfaces.IOverview
import io.ktor.util.*
import javafx.geometry.Orientation
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.logic.ContactController
import modules.mx.isClientGlobal
import modules.mx.rightButtonsWidth
import styling.Stylesheet
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class GContactOverview : IOverview, View("Contacts") {
  private val contactController: ContactController by inject()
  override val root = borderpane {
    right = vbox {
      button("Search") {
        action { contactController.searchEntry() }
        tooltip("Opens the search screen.")
        prefWidth = rightButtonsWidth
      }
      button("New") {
        action { contactController.newEntry() }
        tooltip("Add a new contact to the database.")
        prefWidth = rightButtonsWidth
      }
      button("Save") {
        action { runBlocking { contactController.saveEntry() } }
        tooltip("Saves the current contact.")
        prefWidth = rightButtonsWidth
      }
      button("Analytics") {
        isDisable = isClientGlobal
        action { contactController.openAnalytics() }
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
        action { contactController.openDataImport() }
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
        action { contactController.showEMailer() }
        tooltip("Opens the EMailer.")
        prefWidth = rightButtonsWidth
      }
    }
    center = form {
      contactController.newEntry()
      vbox {
        hbox(10) {
          style {
            unsafe("-fx-background-color", Color.web("#373e43", 1.0))
            paddingAll = 10
          }
          addClass(Stylesheet.fieldsetBorder)
          fieldset("Main Data") {
            add(ContactMainData::class)
            addClass(Stylesheet.fieldsetBorder)
          }
          fieldset("Financial Data") {
            add(ContactFinancialData::class)
            addClass(Stylesheet.fieldsetBorder)
          }
          vbox(10) {
            fieldset("Profession Data") {
              add(ContactProfessionData::class)
              addClass(Stylesheet.fieldsetBorder)
            }
            fieldset("Misc Data") {
              add(ContactMiscData::class)
              addClass(Stylesheet.fieldsetBorder)
              style {
                vgrow = Priority.ALWAYS
              }
            }
          }
          fieldset("Statistics Data") {
            add(ContactStatistics::class)
            addClass(Stylesheet.fieldsetBorder)
          }
        }
      }
    }
  }
}
