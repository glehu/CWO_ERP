package modules.mx.gui

import db.CwODB
import interfaces.IIndexManager
import javafx.collections.ObservableList
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.logic.M1IndexManager
import modules.m2.logic.M2IndexManager
import modules.m3.logic.M3IndexManager
import modules.m4.logic.M4IndexManager
import modules.mx.*
import modules.mx.logic.MXLog
import tornadofx.*

@ExperimentalSerializationApi
class MGXDatabaseManager : View("Databases") {
    /**
     * This list needs to be updated in case a new index manager was added.
     */
    private var indexManagers: ObservableList<IIndexManager> = observableListOf(
        m1GlobalIndex!!, m2GlobalIndex!!, m3GlobalIndex!!, m4GlobalIndex!!
    )

    /**
     * This function needs to be updated in case a new index manager was added.
     */
    private fun updateDatabases() {
        indexManagers.clear()
        m1GlobalIndex = M1IndexManager()
        m2GlobalIndex = M2IndexManager()
        m3GlobalIndex = M3IndexManager()
        m4GlobalIndex = M4IndexManager()
        indexManagers.addAll(m1GlobalIndex, m2GlobalIndex, m3GlobalIndex, m4GlobalIndex)
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
                                logType = MXLog.LogType.INFO,
                                text = "Database ${counter + 1} reset",
                                moduleAlt = "M${counter + 1}"
                            )
                            indexManager.log(
                                logType = MXLog.LogType.INFO,
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
                    MGXLog("MX").showLog(MXLog.getLogFile("MX"), "DATABASE".toRegex())
                }
                prefWidth = rightButtonsWidth
            }
        }
    }

    fun refreshStats() {
        table.refresh()
    }
}
