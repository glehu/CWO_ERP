package modules.mx.gui

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.dataPath
import modules.mx.getIniFile
import modules.mx.logic.getRandomString
import modules.mx.readAndSetIniValues
import tornadofx.*
import java.io.File

class MGXPreferences : View("Preferences")
{
    private val encryptionKeyProperty = SimpleStringProperty(getRandomString(16, true))
    private val dataPathProperty = SimpleStringProperty(System.getProperty("user.dir"))
    private val maxSearchResultsProperty = SimpleIntegerProperty(10_000)
    override val root = form {
        setPrefSize(600.0, 200.0)
        val iniFile = getIniFile()
        if (!iniFile.isFile)
        {
            //Now we have to initialize it
            iniFile.createNewFile()
            iniFile.writeText(
                Json.encodeToString(
                    IniValues(
                        token = encryptionKeyProperty.value,
                        dataPath = dataPathProperty.value,
                        maxSearchResults = maxSearchResultsProperty.value
                    )
                )
            )
        } else
        {
            val iniFileText = iniFile.readText()
            val iniVal = Json.decodeFromString<IniValues>(iniFileText)
            encryptionKeyProperty.value = iniVal.token
            dataPathProperty.value = iniVal.dataPath
            maxSearchResultsProperty.value = iniVal.maxSearchResults
        }
        vbox {
            fieldset {
                field("Encryption Key") { textfield(encryptionKeyProperty) { prefWidth = 200.0 } }
                field("Data Path") {
                    textfield(dataPathProperty) { prefWidth = 200.0 }
                    button("<") {
                        tooltip("Choose Path")
                        action {
                            val dataPathChosen = chooseDirectory("Choose data path", File(dataPath))
                            if (dataPathChosen != null) dataPathProperty.value = dataPathChosen.absolutePath
                        }
                    }
                }
                field("Max Search Results") { textfield(maxSearchResultsProperty) { prefWidth = 200.0 } }
                button("Save") {
                    shortcut("Enter")
                }.action {
                    iniFile.writeText(
                        Json.encodeToString(
                            IniValues(
                                token = encryptionKeyProperty.value,
                                dataPath = dataPathProperty.value,
                                maxSearchResults = maxSearchResultsProperty.value
                            )
                        )
                    )
                    readAndSetIniValues()
                    close()
                }
            }
        }
    }
}

fun showPreferences() = FX.find<MGXPreferences>().openModal(block = true)

@Serializable
data class IniValues(
    @SerialName("encryption key") var token: String,
    @SerialName("data path") var dataPath: String,
    @SerialName("max search results") var maxSearchResults: Int
)