package modules.mx.gui

import api.gui.MGXAPI
import javafx.scene.control.TabPane
import kotlinx.serialization.ExperimentalSerializationApi
import tornadofx.View
import tornadofx.tabpane

class MGXManagement : View("MX Management") {
    @ExperimentalSerializationApi
    override val root = tabpane {
        tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

        tab<MGXUserManager>()
        tab<MGXDatabaseManager>()
        tab<MGXAPI>()
    }
}