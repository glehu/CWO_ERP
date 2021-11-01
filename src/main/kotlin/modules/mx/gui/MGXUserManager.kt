package modules.mx.gui

import io.ktor.util.*
import javafx.collections.ObservableList
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.MXUser
import modules.mx.logic.MXLog
import modules.mx.logic.MXUserManager
import modules.mx.rightButtonsWidth
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class MGXUserManager : View("User Management") {
    private val userManager: MXUserManager by inject()

    @ExperimentalSerializationApi
    private var users: ObservableList<MXUser> = observableListOf(MXUser("", ""))
    private val table = tableview(users) {
        readonlyColumn("Username", MXUser::username).prefWidth(200.0)
        readonlyColumn("Password (encrypted)", MXUser::password)
        readonlyColumn("MX", MXUser::canAccessMX)
            .cellFormat { text = ""; style { backgroundColor = userManager.getRightsCellColor(it) } }
        readonlyColumn("M1", MXUser::canAccessM1)
            .cellFormat { text = ""; style { backgroundColor = userManager.getRightsCellColor(it) } }
        readonlyColumn("M2", MXUser::canAccessM2)
            .cellFormat { text = ""; style { backgroundColor = userManager.getRightsCellColor(it) } }
        readonlyColumn("M3", MXUser::canAccessM3)
            .cellFormat { text = ""; style { backgroundColor = userManager.getRightsCellColor(it) } }
        readonlyColumn("M4", MXUser::canAccessM4)
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
                    MGXLog().showLog(MXLog.getLogFile("MX"), "USER".toRegex())
                }
                prefWidth = rightButtonsWidth
            }
        }
    }

    fun refreshUsers() {
        users = userManager.getUsersObservableList(users, userManager.getCredentials())
        table.items = users
        table.refresh()
    }
}
