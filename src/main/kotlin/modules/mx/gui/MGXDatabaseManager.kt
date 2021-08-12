package modules.mx.gui

import db.CwODB
import javafx.collections.ObservableList
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import modules.IIndexManager
import modules.m1.logic.M1IndexManager
import modules.m2.logic.M2IndexManager
import modules.m3.logic.M3IndexManager
import modules.mx.m1GlobalIndex
import modules.mx.m2GlobalIndex
import modules.mx.m3GlobalIndex
import modules.mx.rightButtonsWidth
import tornadofx.*

@ExperimentalSerializationApi
class MGXDatabaseManager : Fragment("Databases")
{
    private var indexManagers: ObservableList<IIndexManager> = observableListOf(
        m1GlobalIndex, m2GlobalIndex, m3GlobalIndex
    )
    override val root = borderpane {
        right = vbox {
            button("Update Stats") {
                action {
                    updateStats()
                }
                prefWidth = rightButtonsWidth
            }
            button("Reset M1") {
                action {
                    CwODB().resetModuleData("M1")
                    m1GlobalIndex = M1IndexManager()
                    updateStats()
                }
                prefWidth = rightButtonsWidth
                textFill = Color.RED
            }
            button("Reset M2") {
                action {
                    CwODB().resetModuleData("M2")
                    m2GlobalIndex = M2IndexManager()
                    updateStats()
                }
                prefWidth = rightButtonsWidth
                textFill = Color.RED
            }
            button("Reset M3") {
                action {
                    CwODB().resetModuleData("M3")
                    m3GlobalIndex = M3IndexManager()
                    updateStats()
                }
                prefWidth = rightButtonsWidth
                textFill = Color.RED
            }
        }
        center {
            tableview(indexManagers) {
                readonlyColumn("Database", IIndexManager::module).prefWidth(80.0)
                readonlyColumn("Description", IIndexManager::moduleDescription).prefWidth(150.0)
                readonlyColumn("# Entries", IIndexManager::lastUID).prefWidth(125.0)
                readonlyColumn("Last Change", IIndexManager::lastChangeDateLocal).prefWidth(200.0)
                readonlyColumn("by User", IIndexManager::lastChangeUser).prefWidth(200.0)
            }
        }
    }

    private fun updateStats()
    {
        indexManagers.clear()
        indexManagers.addAll(m1GlobalIndex, m2GlobalIndex, m3GlobalIndex)
    }
}
