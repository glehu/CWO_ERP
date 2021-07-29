package modules.mx.gui

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.logic.getRandomString
import modules.mx.maxSearchResultsGlobal
import modules.mx.programPath
import modules.mx.token
import tornadofx.*
import java.io.File

class MGXPreferences : View("Preferences")
{
    private val encryptionKey = SimpleStringProperty(getRandomString(16, true))
    private val dataPath = SimpleStringProperty(System.getProperty("user.dir"))
    private val maxSearchResults = SimpleIntegerProperty(10_000)
    override val root = form {
        setPrefSize(300.0, 200.0)
        val iniFile = File("$programPath\\cwo_erp.ini")
        vbox {
            fieldset {
                field("Encryption Key") { textfield(encryptionKey) { prefWidth = 200.0 }.required() }
                field("Data Path") { textfield(dataPath) { prefWidth = 200.0 }.required() }
                field("Max Search Results") { textfield(maxSearchResults) { prefWidth = 200.0 }.required() }
                button("Save").action {
                    iniFile.createNewFile()
                    //Now we have to initialize it
                    iniFile.writeText(
                        Json.encodeToString(
                            IniValues(
                                token = encryptionKey.value,
                                dataPath = dataPath.value,
                                maxSearchResults = maxSearchResults.value
                            )
                        )
                    )
                    modules.mx.dataPath = encryptionKey.value
                    token = dataPath.value
                    maxSearchResultsGlobal = maxSearchResults.value
                }
            }
        }
    }
    @Serializable
    private data class IniValues(
        @SerialName("encryption key") var token: String,
        @SerialName("data path") var dataPath: String,
        @SerialName("max search results") var maxSearchResults: Int
    )
}