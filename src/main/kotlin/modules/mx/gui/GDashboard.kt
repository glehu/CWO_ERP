package modules.mx.gui

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.User
import modules.mx.logic.UserManager
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class GDashboard : View("Dashboard") {
  private val userManager: UserManager by inject()
  private val dbManager = find<GDatabaseManager>()
  private val activeUsers = tableview(userManager.getActiveUsers()) {
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

  fun update() {
    find<GDashboard>().activeUsers.items = userManager.getActiveUsers()
    find<GDashboard>().activeUsers.refresh()
  }
}
