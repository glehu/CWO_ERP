package modules.m4.gui

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import javafx.scene.control.TabPane
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m4.misc.ItemIni
import modules.mx.invoiceIndexManager
import modules.mx.rightButtonsWidth
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class GItemSettings : IModule, Fragment("Inventory Settings") {
    override val moduleNameLong = "ItemSettings"
    override val module = "M4"
    override fun getIndexManager(): IIndexManager {
        return invoiceIndexManager!!
    }

    override val root = borderpane {
        prefWidth = 800.0
        prefHeight = 500.0
        center = tabpane {
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            tab("Stock Settings") {
                form {
                    fieldset("Requires Rights") {
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
                getSettingsFile().writeText(
                    Json.encodeToString(
                        ItemIni()
                    )
                )
                close()
            }
        }
    }
}
