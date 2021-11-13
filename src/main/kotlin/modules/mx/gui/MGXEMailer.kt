package modules.mx.gui

import api.misc.json.MGXEMailerIni
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.scene.paint.Color
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m2.M2Contact
import modules.m2.logic.M2Controller
import modules.m4.Statistic
import modules.mx.m2GlobalIndex
import modules.mx.rightButtonsWidth
import tornadofx.*

@ExperimentalSerializationApi
@InternalAPI
class MGXEMailer : IModule, View("EMailer") {
    override val moduleNameLong = "MGXEMailer"
    override val module = "MX"
    override fun getIndexManager(): IIndexManager? {
        return null
    }

    private var iniVal = getIni()

    private val subjectProperty = SimpleStringProperty()
    val salutationProperty = SimpleStringProperty()
    private val bodyProperty = SimpleStringProperty(getDefaultTexts())
    val recipientProperty = SimpleStringProperty()
    var contact: M2Contact? = null

    private fun getDefaultTexts(): String {
        iniVal = getIni()
        var defaultTextBody = ""
        if (iniVal.defaultFooter.isNotEmpty()) {
            defaultTextBody = "\n\n${iniVal.defaultFooter}"
        }
        return defaultTextBody
    }

    private val statusProperty = SimpleStringProperty("Draft")

    private val m2controller: M2Controller by inject()
    override val root = borderpane {
        prefWidth = 800.0
        prefHeight = 500.0
        center = form {
            fieldset("EMail Form") {
                field("Status") { label(statusProperty) }
                field("Subject") { textfield(subjectProperty) }
                field("Salutation") { textfield(salutationProperty) }
                field("Body") { textarea(bodyProperty) }
                field("Recipient") {
                    textfield(recipientProperty)
                    button("Load Contact") {
                        action {
                            contact = m2controller.selectAndLoadContact()
                            recipientProperty.value = contact!!.email
                            salutationProperty.value = contact!!.salutation
                        }
                    }
                }
            }
        }
        right = vbox {
            button("Send EMail (CTRL+S)") {
                shortcut("CTRL+S")
                prefWidth = rightButtonsWidth * 1.5
                style { unsafe("-fx-base", Color.DARKGREEN) }
                action {
                    if (sendEMail(
                            subject = subjectProperty.value,
                            body = getBodyText(),
                            recipient = recipientProperty.value
                        )) {
                        statusProperty.value = "Sent"
                        //Add Contact Statistic
                        if (contact != null) {
                            val statistic: Statistic = if (contact!!.statistics.containsKey("EMails Sent")) {
                                Json.decodeFromString(contact!!.statistics["EMails Sent"]!!)
                            } else {
                                Statistic("EMails Sent", "0", 0.0F, true)
                            }
                            statistic.nValue += 1.0F
                            statistic.sValue = (statistic.nValue).toString()
                            contact!!.statistics["EMails Sent"] = Json.encodeToString(statistic)
                            runBlocking {
                                m2GlobalIndex!!.save(contact as M2Contact)
                            }
                        }
                    } else statusProperty.value = "Draft (Error while sending!)"
                }
            }
            button("New EMail (CTRL+X)") {
                shortcut("CTRL+X")
                prefWidth = rightButtonsWidth * 1.5
                action {
                    subjectProperty.value = ""
                    salutationProperty.value = ""
                    bodyProperty.value = getDefaultTexts()
                    recipientProperty.value = ""
                    statusProperty.value = "Draft"
                }
            }
            // ##################################################
            // ################## Settings ######################
            // ##################################################
            separator(Orientation.HORIZONTAL) {
                paddingVertical = 15
            }
            button("Settings") {
                action { showSettings() }
                tooltip("Shows the settings.")
                prefWidth = rightButtonsWidth * 1.5
            }
        }
    }

    private fun showSettings() {
        find<MGXEMailerSettings>().openModal()
    }

    private fun getIni(): MGXEMailerIni {
        val iniFile = getSettingsFile(subSetting = "MGXEMailer")
        val iniTxt = iniFile.readText()
        return if (iniTxt.isNotEmpty()) Json.decodeFromString(iniTxt) else MGXEMailerIni()
    }

    private fun getBodyText(): String {
        return "${salutationProperty.value}\n\n${bodyProperty.value}"
    }
}
