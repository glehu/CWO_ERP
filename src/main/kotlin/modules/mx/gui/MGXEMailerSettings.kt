package modules.mx.gui

import api.misc.json.MGXEMailerIni
import interfaces.IIndexManager
import interfaces.IModule
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.TabPane
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.rightButtonsWidth
import tornadofx.*

@ExperimentalSerializationApi
class MGXEMailerSettings : IModule, Fragment("EMailer Settings") {
    override val moduleNameLong = "MGXEMailerSettings"
    override val module = "MX"
    override fun getIndexManager(): IIndexManager? {
        return null
    }

    private val iniVal = getIni()

    private fun getIni(): MGXEMailerIni {
        val iniFile = getSettingsFile(subSetting = "MGXEMailer")
        val iniTxt = iniFile.readText()
        return if (iniTxt.isNotEmpty()) Json.decodeFromString(iniTxt) else MGXEMailerIni()
    }

    private val defaultFooterProperty = SimpleStringProperty(iniVal.defaultFooter)

    override val root = borderpane {
        prefWidth = 800.0
        prefHeight = 500.0
        center = tabpane {
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
        }
        bottom = hbox {
            button("Save (Enter)") {
                prefWidth = rightButtonsWidth
                shortcut("Enter")
            }.action {
                getSettingsFile(subSetting = "MGXEMailer").writeText(
                    Json.encodeToString(
                        MGXEMailerIni(
                            defaultFooter = defaultFooterProperty.value,
                        )
                    )
                )
                close()
            }
        }
    }
}
