package modules.mx.gui

import db.CwODB
import interfaces.IIndexManager
import javafx.collections.ObservableList
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.logic.M1IndexManager
import modules.m2.logic.M2IndexManager
import modules.m3.logic.M3IndexManager
import modules.mx.*
import tornadofx.*

@ExperimentalSerializationApi
class MGXDatabaseManager : View("Databases") {
    private var indexManagers: ObservableList<IIndexManager> = observableListOf(
        m1GlobalIndex, m2GlobalIndex, m3GlobalIndex
    )
    override val root = borderpane {
        right = vbox {
            button("Update Databases") {
                action {
                    updateDatabases()
                }
                prefWidth = rightButtonsWidth
            }
            button("Reset M1") {
                action {
                    CwODB().resetModuleDatabase("M1")
                    m1GlobalIndex = M1IndexManager()
                    m1GlobalIndex.setLastChangeData(-1, activeUser)
                    updateDatabases()
                }
                prefWidth = rightButtonsWidth
                style { unsafe("-fx-base", Color.DARKRED) }
            }
            button("Reset M2") {
                action {
                    CwODB().resetModuleDatabase("M2")
                    m2GlobalIndex = M2IndexManager()
                    m2GlobalIndex.setLastChangeData(-1, activeUser)
                    updateDatabases()
                }
                prefWidth = rightButtonsWidth
                style { unsafe("-fx-base", Color.DARKRED) }
            }
            button("Reset M3") {
                action {
                    CwODB().resetModuleDatabase("M3")
                    m3GlobalIndex = M3IndexManager()
                    m3GlobalIndex.setLastChangeData(-1, activeUser)
                    updateDatabases()
                }
                prefWidth = rightButtonsWidth
                style { unsafe("-fx-base", Color.DARKRED) }
            }
        }
        center {
            tableview(indexManagers) {
                readonlyColumn("Database", IIndexManager::module).prefWidth(80.0)
                readonlyColumn("Description", IIndexManager::moduleDescription).prefWidth(150.0)
                readonlyColumn("# Entries", IIndexManager::lastUID).prefWidth(125.0)
                readonlyColumn("Last Change", IIndexManager::lastChangeDateLocal).prefWidth(175.0)
                readonlyColumn("by User", IIndexManager::lastChangeUser).prefWidth(200.0)
            }
        }
    }

    private fun updateDatabases() {
        indexManagers.clear()
        m1GlobalIndex = M1IndexManager()
        m2GlobalIndex = M2IndexManager()
        m3GlobalIndex = M3IndexManager()
        indexManagers.addAll(m1GlobalIndex, m2GlobalIndex, m3GlobalIndex)
    }
}
