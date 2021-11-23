package modules.m3.gui

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.TabPane
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m3.logic.M3CLIController
import modules.m3.misc.M3Ini
import modules.m4.Statistic
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

    private var statusTexts = observableListOf<Statistic>()
    private val todoStatuses = SimpleStringProperty(iniVal.todoStatuses)
    private val autoCommission = SimpleBooleanProperty(iniVal.autoCommission)
    private val autoCreateContacts = SimpleBooleanProperty(iniVal.autoCreateContacts)
    private val autoSendEMailConfirmation = SimpleBooleanProperty(iniVal.autoSendEMailConfirmation)

    override val root = borderpane {
        prefWidth = 800.0
        prefHeight = 500.0
        for ((n, s) in iniVal.statusTexts) {
            val element = Statistic(
                description = "Mapping",
                sValue = s,
                nValue = n.toFloat(),
                number = false
            )
            statusTexts.add(element)
        }
        center = tabpane {
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            tab("Status Settings") {
                form {
                    fieldset("Status Texts") {
                        tableview(statusTexts) {
                            isEditable = true
                            readonlyColumn("Status", Statistic::nValue)
                            column("Text", Statistic::sValue) {
                                makeEditable()
                            }
                            enableCellEditing()
                            isFocusTraversable = false
                        }
                    }
                    fieldset("To Do Quicksearch") {
                        field("Statuses") {
                            textfield(todoStatuses) {
                                tooltip("Displays the To Do relevant statuses, seperated by a comma <,>.")
                            }
                        }
                    }
                }
            }
            tab("API Settings") {
                form {
                    fieldset("Automatic Processing") {
                        field("Auto-Commission Web Orders") {
                            checkbox(property = autoCommission) {
                                tooltip("When checked, puts web orders into status 1.")
                            }
                        }
                        field("Auto-Create new Contacts") {
                            checkbox(property = autoCreateContacts)
                        }
                        field("Auto-Send EMail Confirmation") {
                            checkbox(property = autoSendEMailConfirmation)
                        }
                    }
                }
            }
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
        }
        bottom = hbox {
            button("Save (CTRL+S)") {
                prefWidth = rightButtonsWidth
                shortcut("CTRL+S")
            }.action {
                val newMap = mutableMapOf<Int, String>()
                for (statistic in statusTexts) {
                    newMap[statistic.nValue.toInt()] = statistic.sValue
                }
                getSettingsFile().writeText(
                    Json.encodeToString(
                        M3Ini(
                            statusTexts = newMap,
                            todoStatuses = todoStatuses.value,
                            autoCommission = autoCommission.value,
                            autoCreateContacts = autoCreateContacts.value,
                            autoSendEMailConfirmation = autoSendEMailConfirmation.value
                        )
                    )
                )
                close()
            }
        }
    }
}
