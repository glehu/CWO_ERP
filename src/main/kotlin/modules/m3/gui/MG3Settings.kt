package modules.m3.gui

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import javafx.beans.property.SimpleBooleanProperty
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m3.M3Ini
import modules.m3.logic.M3CLIController
import modules.mx.getIniFile
import modules.mx.m3GlobalIndex
import modules.mx.rightButtonsWidth
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class MG3Settings : IModule, Fragment("M3 Settings") {
    override val moduleNameLong = "MG3Settings"
    override val module = "M3"
    override fun getIndexManager(): IIndexManager {
        return m3GlobalIndex!!
    }

    private val iniVal = M3CLIController().getIni()

    private val autoCreateContacts = SimpleBooleanProperty(iniVal.autoCreateContacts)
    override val root = borderpane {
        prefWidth = 800.0
        prefHeight = 500.0
        center = tabpane {
            tab("Database & Indices") {
                form {
                    fieldset("Indices") {
                        button("Rebuild indices") {
                            //TODO: Not yet implemented
                            isDisable = true
                            tooltip("Rebuilds all indices in case of faulty indices.")
                            prefWidth = rightButtonsWidth
                        }
                    }
                }
            }
            tab("API Settings") {
                form {
                    fieldset("Automatic Processing") {
                        field("Auto-Create new Contacts") {
                            checkbox(property = autoCreateContacts)
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
                getIniFile().writeText(
                    Json.encodeToString(
                        M3Ini(
                            autoCreateContacts = autoCreateContacts.value
                        )
                    )
                )
                close()
            }
        }
    }
}
