package modules.mx.gui

import api.gui.GAPI
import io.ktor.util.*
import javafx.scene.control.TabPane
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.isClientGlobal
import tornadofx.View
import tornadofx.tabpane

@InternalAPI
@ExperimentalSerializationApi
class GManagement : View("MX Management") {
    override val root = tabpane {
        tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

        tab<GDashboard>()
        tab<GUserManager>()
        tab<GDatabaseManager>()
        if (!isClientGlobal) tab<GAPI>() //TODO client work
    }
}
