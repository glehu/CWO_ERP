package modules.mx.gui

import io.ktor.util.*
import javafx.collections.ObservableList
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.User
import modules.mx.logic.Log
import modules.mx.logic.UserManager
import modules.mx.rightButtonsWidth
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class GUserManager : View("User Management") {
  private val userManager: UserManager by inject()

  @ExperimentalSerializationApi
  private var users: ObservableList<User> = observableListOf(User("", ""))
  private val table = tableview(users) {
    readonlyColumn("Username", User::username).prefWidth(200.0)
    readonlyColumn("Password (encrypted)", User::password)
    readonlyColumn("Management", User::canAccessManagement)
      .cellFormat { text = ""; style { backgroundColor = userManager.getRightsCellColor(it) } }
    readonlyColumn("Discography", User::canAccessDiscography)
      .cellFormat { text = ""; style { backgroundColor = userManager.getRightsCellColor(it) } }
    readonlyColumn("Contacts", User::canAccessContacts)
      .cellFormat { text = ""; style { backgroundColor = userManager.getRightsCellColor(it) } }
    readonlyColumn("Invoices", User::canAccessInvoices)
      .cellFormat { text = ""; style { backgroundColor = userManager.getRightsCellColor(it) } }
    readonlyColumn("Inventory", User::canAccessInventory)
      .cellFormat { text = ""; style { backgroundColor = userManager.getRightsCellColor(it) } }
    onUserSelect(1) {
      userManager.showUser(it, userManager.getCredentials(), users)
    }
  }
  override val root = borderpane {
    refreshUsers()
    center = table
    right = vbox {
      button("Add user") {
        action {
          userManager.addUser(userManager.getCredentials(), users)
        }
        prefWidth = rightButtonsWidth
      }
      button("Show log") {
        action {
          GLog("MX").showLog(Log.getLogFile("MX"), "USER".toRegex())
        }
        prefWidth = rightButtonsWidth
      }
    }
  }

  private fun refreshUsers() {
    users = userManager.getUsersObservableList(users, userManager.getCredentials())
    table.items = users
    table.refresh()
  }
}
