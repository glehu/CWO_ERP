package modules.mx.gui

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.TabPane
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.misc.EMailerIni
import modules.mx.rightButtonsWidth
import tornadofx.Fragment
import tornadofx.action
import tornadofx.borderpane
import tornadofx.button
import tornadofx.checkbox
import tornadofx.field
import tornadofx.fieldset
import tornadofx.form
import tornadofx.hbox
import tornadofx.tab
import tornadofx.tabpane
import tornadofx.textarea
import tornadofx.tooltip

@InternalAPI
@ExperimentalSerializationApi
class GEMailerSettings : IModule, Fragment("EMailer Settings") {
  override val moduleNameLong = "EMailerSettings"
  override val module = "MX"
  override fun getIndexManager(): IIndexManager? {
    return null
  }

  private val iniVal = getIni()

  private fun getIni(): EMailerIni {
    val iniFile = getSettingsFile(subSetting = "MGXEMailer")
    val iniTxt = iniFile.readText()
    return if (iniTxt.isNotEmpty()) Json.decodeFromString(iniTxt) else EMailerIni()
  }

  private val defaultFooterProperty = SimpleStringProperty(iniVal.defaultFooter)
  private val writeStatistics = SimpleBooleanProperty(iniVal.writeStatistics)

  override val root = borderpane {
    prefWidth = 800.0
    prefHeight = 400.0
    left = tabpane {
      tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
      tab("Default Texts") {
        form {
          fieldset("Footer") {
            field("Default Footer") {
              textarea(defaultFooterProperty)
            }
          }
        }
      }
      tab("Automatic Processing") {
        form {
          fieldset("Statistics") {
            field("Write Statistics") {
              checkbox(property = writeStatistics) {
                tooltip(
                  "When checked, keeps track of the amount of emails sent to each contact.\n" +
                          "Amount of EMails sent will be written into the contacts statistics."
                )
              }
            }
          }
        }
      }
    }
    bottom = hbox {
      button("Save (CTRL+S)") {
        prefWidth = rightButtonsWidth
        shortcut("CTRL+S")
      }.action {
        getSettingsFile(subSetting = "MGXEMailer").writeText(
          Json.encodeToString(
            EMailerIni(
              defaultFooter = defaultFooterProperty.value,
              writeStatistics = writeStatistics.value
            )
          )
        )
        close()
      }
    }
  }
}
