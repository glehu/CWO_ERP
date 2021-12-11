package modules.mx.gui

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.User
import modules.mx.logic.MXUserManager
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class GDashboard : View("Dashboard") {
    private val userManager: MXUserManager by inject()
    private val dbManager = find<GDatabaseManager>()
    val activeUsers = tableview(userManager.getActiveUsers()) {
        readonlyColumn("Username", User::username).prefWidth(175.0)
        readonlyColumn("Online since", User::onlineSince).prefWidth(200.0)
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
