package modules.mx.gui

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.MXIni
import modules.mx.getIniFile
import modules.mx.logic.getRandomString
import modules.mx.logic.readAndSetIniValues
import tornadofx.*

@ExperimentalSerializationApi
class MGXPreferences : View("Preferences") {
    private val encryptionKeyProperty = SimpleStringProperty(getRandomString(16, true))
    private val dataPathProperty = SimpleStringProperty(System.getProperty("user.dir"))
    private val maxSearchResultsProperty = SimpleIntegerProperty(2_000)
    private val differenceFromUTCProperty = SimpleIntegerProperty(0)
    private val isClientProperty = SimpleStringProperty("false")
    private val serverIPAddressProperty = SimpleStringProperty("?")
    private val emailUsernameProperty = SimpleStringProperty("?")
    private val emailPasswordProperty = SimpleStringProperty("?")
    private val emailHostProperty = SimpleStringProperty("?")
    private val emailPortProperty = SimpleStringProperty("?")
    private val jsonSerializer = Json {
        prettyPrint = true
    }
    override val root = form {
        setPrefSize(600.0, 200.0)
        val iniFile = getIniFile()
        if (!iniFile.isFile) {
            //Now we have to initialize it
            iniFile.createNewFile()
            iniFile.writeText(
                jsonSerializer.encodeToString(
                    MXIni(
                        token = encryptionKeyProperty.value,
                        dataPath = dataPathProperty.value,
                        maxSearchResults = maxSearchResultsProperty.value,
                        differenceFromUTC = differenceFromUTCProperty.value,
                        isClient = isClientProperty.value.toBoolean(),
                        serverIPAddress = serverIPAddressProperty.value,
                        emailUsername = emailUsernameProperty.value,
                        emailPassword = emailPasswordProperty.value,
                        emailHost = emailHostProperty.value,
                        emailPort = emailPortProperty.value
                    )
                )
            )
        } else {
            val iniFileText = iniFile.readText()
            val iniVal = jsonSerializer.decodeFromString<MXIni>(iniFileText)
            encryptionKeyProperty.value = iniVal.token
            dataPathProperty.value = iniVal.dataPath
            maxSearchResultsProperty.value = iniVal.maxSearchResults
            differenceFromUTCProperty.value = iniVal.differenceFromUTC
            isClientProperty.value = iniVal.isClient.toString()
            serverIPAddressProperty.value = iniVal.serverIPAddress
            emailUsernameProperty.value = iniVal.emailUsername
            emailPasswordProperty.value = iniVal.emailPassword
            emailHostProperty.value = iniVal.emailHost
            emailPortProperty.value = iniVal.emailPort
        }
        vbox {
            fieldset {
                field("Encryption key") {
                    textfield(encryptionKeyProperty) { prefWidth = 200.0 }
                }
                field("Data path") {
                    textfield(dataPathProperty) { prefWidth = 200.0 }
                    button("<") {
                        tooltip("Choose path")
                        action {
                            val dataPathChosen = chooseDirectory("Choose data path") //, File(dataPath))
                            if (dataPathChosen != null) dataPathProperty.value = dataPathChosen.absolutePath
                        }
                    }
                }
                field("Max search results") {
                    textfield(maxSearchResultsProperty) { prefWidth = 200.0 }
                }
                field("Difference from UTC in hours") {
                    textfield(differenceFromUTCProperty) { prefWidth = 200.0 }
                }
                field("Is client") {
                    combobox(isClientProperty, listOf("true", "false")) { prefWidth = 200.0 }
                }
                field("Server IP Address") {
                    textfield(serverIPAddressProperty) { prefWidth = 200.0 }
                }
                field("EMail Address") {
                    textfield(emailUsernameProperty) { prefWidth = 200.0 }
                }
                field("SMTP Password") {
                    textfield(emailPasswordProperty) { prefWidth = 200.0 }
                }
                button("Save") {
                    shortcut("Enter")
                }.action {
                    iniFile.writeText(
                        jsonSerializer.encodeToString(
                            MXIni(
                                token = encryptionKeyProperty.value,
                                dataPath = dataPathProperty.value,
                                maxSearchResults = maxSearchResultsProperty.value,
                                differenceFromUTC = differenceFromUTCProperty.value,
                                isClient = isClientProperty.value.toBoolean(),
                                serverIPAddress = serverIPAddressProperty.value,
                                emailUsername = emailUsernameProperty.value,
                                emailPassword = emailPasswordProperty.value,
                                emailHost = emailHostProperty.value,
                                emailPort = emailPortProperty.value
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

@ExperimentalSerializationApi
fun showPreferences() = FX.find<MGXPreferences>().openModal(block = true)
