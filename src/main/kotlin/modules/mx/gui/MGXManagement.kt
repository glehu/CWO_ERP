package modules.mx.gui

import api.gui.MGXAPI
import io.ktor.util.*
import javafx.scene.control.TabPane
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.isClientGlobal
import tornadofx.View
import tornadofx.tabpane

@InternalAPI
@ExperimentalSerializationApi
class MGXManagement : View("MX Management") {
    override val root = tabpane {
        tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

        tab<MGXUserManager>()
        tab<MGXDatabaseManager>()
        if (!isClientGlobal) tab<MGXAPI>() //TODO client work
    }
}