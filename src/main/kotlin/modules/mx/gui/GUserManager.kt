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
        readonlyColumn("MX", User::canAccessMX)
            .cellFormat { text = ""; style { backgroundColor = userManager.getRightsCellColor(it) } }
        readonlyColumn("M1", User::canAccessM1)
            .cellFormat { text = ""; style { backgroundColor = userManager.getRightsCellColor(it) } }
        readonlyColumn("M2", User::canAccessM2)
            .cellFormat { text = ""; style { backgroundColor = userManager.getRightsCellColor(it) } }
        readonlyColumn("M3", User::canAccessM3)
            .cellFormat { text = ""; style { backgroundColor = userManager.getRightsCellColor(it) } }
        readonlyColumn("M4", User::canAccessM4)
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
