package modules.m2.gui

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.stage.FileChooser
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.logic.M2Import
import modules.m2.misc.ContactModel
import modules.mx.gui.MGXProgressbar
import tornadofx.*
import java.io.File

@ExperimentalSerializationApi
class MG2Import : Fragment("Contact Data Import")
{
    private val contactSchema: ContactModel by inject()
    private val m2controller: M2Import by inject()
    private val progressProperty = SimpleIntegerProperty()
    private val filenameProperty = SimpleStringProperty()
    private lateinit var file: File
    private var progressN by progressProperty
    private val buttonWidth = 150.0
    private val birthdayHeaderName = SimpleStringProperty()
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
                        if (file.isFile) filenameProperty.value = file.name
                    }
                    prefWidth = buttonWidth
                }
                label(filenameProperty) {
                    paddingHorizontal = 20
                }
            }
            button("Start") {
                action {
                    runAsync {
                        m2controller.importData(file, contactSchema, birthdayHeaderName.value) {
                            progressN = it.first
                            updateProgress(it.first.toDouble(), it.first.toDouble())
                            updateMessage("${it.second} ${it.first}")
                        }
                    }
                }
                prefWidth = buttonWidth
            }
            add<MGXProgressbar>()

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
}