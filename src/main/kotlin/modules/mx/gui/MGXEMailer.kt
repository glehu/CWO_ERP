package modules.mx.gui

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import javafx.beans.property.SimpleStringProperty
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.logic.M2Controller
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

    private val subjectProperty = SimpleStringProperty()
    private val bodyProperty = SimpleStringProperty()
    private val recipientProperty = SimpleStringProperty()

    private val statusProperty = SimpleStringProperty("Draft")

    private val m2controller: M2Controller by inject()
    override val root = borderpane {
        center = form {
            fieldset("EMail Form") {
                field("Status") { label(statusProperty) }
                field("Subject") { textfield(subjectProperty) }
                field("Body") { textarea(bodyProperty) }
                field("Recipient") {
                    textfield(recipientProperty)
                    button("Load Contact") {
                        action {
                            val contact = m2controller.selectAndReturnContact()
                            recipientProperty.value = contact.email
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
                            body = bodyProperty.value,
                            recipient = recipientProperty.value
                        )) {
                        statusProperty.value = "Sent"
                    } else statusProperty.value = "Draft (Error while sending!)"
                }
            }
            button("New EMail (CTRL+X)") {
                shortcut("CTRL+X")
                prefWidth = rightButtonsWidth * 1.5
                action {
                    subjectProperty.value = ""
                    bodyProperty.value = ""
                    recipientProperty.value = ""
                    statusProperty.value = "Draft"
                }
            }
        }
    }
}
