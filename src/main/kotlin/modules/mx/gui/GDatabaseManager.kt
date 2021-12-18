package modules.mx.gui

import db.CwODB
import interfaces.IIndexManager
import io.ktor.util.*
import javafx.collections.ObservableList
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.logic.DiscographyIndexManager
import modules.m2.logic.ContactIndexManager
import modules.m3.logic.InvoiceIndexManager
import modules.m4.logic.ItemIndexManager
import modules.m4.logic.ItemStockPostingIndexManager
import modules.mx.*
import modules.mx.logic.Log
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class GDatabaseManager : View("Databases") {
    /**
     * This list needs to be updated in case a new index manager was added.
     */
    private var indexManagers: ObservableList<IIndexManager> = observableListOf()

    /**
     * This function needs to be updated in case a new index manager was added.
     */
    private fun updateDatabases() {
        indexManagers.clear()
        discographyIndexManager = DiscographyIndexManager()
        contactIndexManager = ContactIndexManager()
        invoiceIndexManager = InvoiceIndexManager()
        itemIndexManager = ItemIndexManager()
        itemStockPostingIndexManager = ItemStockPostingIndexManager()
        indexManagers.addAll(discographyIndexManager, contactIndexManager, invoiceIndexManager, itemIndexManager, itemStockPostingIndexManager)
    }

    init {
        if (discographyIndexManager != null) indexManagers.add(discographyIndexManager!!)
        if (contactIndexManager != null) indexManagers.add(contactIndexManager!!)
        if (invoiceIndexManager != null) indexManagers.add(invoiceIndexManager!!)
        if (itemIndexManager != null) indexManagers.add(itemIndexManager!!)
        if (itemStockPostingIndexManager != null) indexManagers.add(itemStockPostingIndexManager!!)
    }

    val table = tableview(indexManagers) {
        readonlyColumn("Database", IIndexManager::module).prefWidth(80.0)
        readonlyColumn("Description", IIndexManager::moduleNameLong).prefWidth(150.0)
        readonlyColumn("# Entries", IIndexManager::lastUID).prefWidth(125.0)
        readonlyColumn("DB Size (KiB)", IIndexManager::dbSizeKiByte).prefWidth(125.0)
        readonlyColumn("Index Size (KiB)", IIndexManager::ixSizeKiByte).prefWidth(125.0)
        readonlyColumn("Last Change", IIndexManager::lastChangeDateLocal).prefWidth(175.0)
        readonlyColumn("by User", IIndexManager::lastChangeUser).prefWidth(175.0)
    }

    override val root = borderpane {
        center = form {
            vbox {
                button("Update Databases") {
                    action {
                        updateDatabases()
                    }
                    prefWidth = rightButtonsWidth
                }
                for ((counter, indexManager) in indexManagers.withIndex()) {
                    button("Reset M${counter + 1}") {
                        action {
                            CwODB.resetModuleDatabase("M${counter + 1}")
                            indexManager.setLastChangeData(-1, activeUser.username)
                            updateDatabases()
                            indexManager.log(
                                logType = Log.LogType.INFO,
                                text = "Database ${counter + 1} reset",
                                moduleAlt = "M${counter + 1}"
                            )
                            indexManager.log(
                                logType = Log.LogType.INFO,
                                text = "Database ${counter + 1} reset",
                                moduleAlt = "MX"
                            )
                        }
                        prefWidth = rightButtonsWidth
                        style { unsafe("-fx-base", Color.DARKRED) }
                    }
                }
            }
        }
        right = vbox {
            button("Show log") {
                action {
                    GLog("MX").showLog(Log.getLogFile("MX"), "DATABASE".toRegex())
                }
                prefWidth = rightButtonsWidth
            }
        }
    }

    fun refreshStats() {
        table.refresh()
    }
}