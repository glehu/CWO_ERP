package modules.mx.gui

import db.CwODB
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
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
    override val root = borderpane {
        right = vbox {
            button("Reset M1") {
                action {
                    CwODB().resetModuleData("M1")
                    m1GlobalIndex = M1IndexManager()
                }
                prefWidth = rightButtonsWidth
                textFill = Color.RED
            }
            button("Reset M2") {
                action {
                    CwODB().resetModuleData("M2")
                    m2GlobalIndex = M2IndexManager()
                }
                prefWidth = rightButtonsWidth
                textFill = Color.RED
            }
            button("Reset M3") {
                action {
                    CwODB().resetModuleData("M3")
                    m3GlobalIndex = M3IndexManager()
                }
                prefWidth = rightButtonsWidth
                textFill = Color.RED
            }
        }
    }
}
