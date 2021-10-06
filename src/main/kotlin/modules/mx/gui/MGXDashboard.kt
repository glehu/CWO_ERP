package modules.mx.gui

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.MXUser
import modules.mx.logic.MXUserManager
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class MGXDashboard : View("Dashboard") {
    private val userManager: MXUserManager by inject()
    private val dbManager = find<MGXDatabaseManager>()
    val activeUsers = tableview(userManager.getActiveUsers()) {
        readonlyColumn("Username", MXUser::username).prefWidth(175.0)
        readonlyColumn("Online since", MXUser::onlineSince).prefWidth(200.0)
    }
    override val root = vbox {
        hbox(10) {
            fieldset(dbManager.title) {
                add(dbManager.table)
            }
            fieldset("Active Users") {
                add(activeUsers)
            }
        }
    }
}