package modules.mx.gui

import javafx.scene.control.TabPane
import kotlinx.serialization.ExperimentalSerializationApi
import tornadofx.Fragment
import tornadofx.tabpane

class MGXManagement : Fragment("MX Management")
{
    @ExperimentalSerializationApi
    override val root = tabpane {
        tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

        tab<MGXUserManager>()
        tab<MGXDatabaseManager>()
    }
}