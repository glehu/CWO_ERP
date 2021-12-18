package modules.m2.gui

import io.ktor.util.*
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Button
import javafx.stage.FileChooser
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.logic.ContactImport
import modules.m2.misc.ContactModel
import modules.mx.gui.GProgressbar
import tornadofx.*
import java.io.File

@InternalAPI
@ExperimentalSerializationApi
class GContactImport : Fragment("Contact Import") {
    private val contactSchema: ContactModel = ContactModel()
    private val m2controller: ContactImport by inject()
    private val progressProperty = SimpleIntegerProperty()
    private val filenameProperty = SimpleStringProperty()
    private lateinit var file: File
    private var progressN by progressProperty
    private val buttonWidth = 150.0
    private val birthdayHeaderName = SimpleStringProperty()
    private lateinit var startButton: Button
    override val root = form {
        setPrefSize(400.0, 600.0)
        vbox {
            hbox {
                button("Choose file") {
                    action {
                        file = chooseFile(
                            "Choose file",
                            arrayOf(FileChooser.ExtensionFilter("CSV file (*.csv)", "*.csv")),
                            mode = FileChooserMode.Single
                        )[0]
                        if (file.isFile) {
                            filenameProperty.value = file.name
                            startButton.isDisable = false
                        }
                    }
                    prefWidth = buttonWidth
                }
                label(filenameProperty) {
                    paddingHorizontal = 20
                }
            }
            startButton = button("Start") {
                isDisable = (filenameProperty.value == null || filenameProperty.value.isEmpty())
                action {
                    runAsync {
                        runBlocking {
                            m2controller.importData(file, contactSchema, birthdayHeaderName.value) {
                                progressN = it.first
                                updateProgress(it.first.toDouble(), it.first.toDouble())
                                updateMessage("${it.second} ${it.first}")
                            }
                        }
                    }
                }
            }
            prefWidth = buttonWidth
        }
        add<GProgressbar>()

        //Custom import scheme
        contactSchema.name.value = "name"
        contactSchema.firstName.value = "firstname"
        contactSchema.lastName.value = "lastname"
        contactSchema.street.value = "street"
        contactSchema.city.value = "city"
        contactSchema.postCode.value = "postcode"
        contactSchema.country.value = "country"
        birthdayHeaderName.value = "birthdate"
        fieldset("Header column names") {
            field("Name") {
                textfield(contactSchema.name) { prefWidth = 50.0 }
            }
            field("First Name") {
                textfield(contactSchema.firstName) { prefWidth = 50.0 }
            }
            field("Last Name") {
                textfield(contactSchema.lastName) { prefWidth = 50.0 }
            }
            field("Street") {
                textfield(contactSchema.street) { prefWidth = 50.0 }
            }
            field("City") {
                textfield(contactSchema.city) { prefWidth = 50.0 }
            }
            field("Post Code") {
                textfield(contactSchema.postCode) { prefWidth = 50.0 }
            }
            field("Country") {
                textfield(contactSchema.country) { prefWidth = 50.0 }
            }
            field("Birthdate") {
                textfield(birthdayHeaderName) { prefWidth = 50.0 }
            }
        }
    }
}