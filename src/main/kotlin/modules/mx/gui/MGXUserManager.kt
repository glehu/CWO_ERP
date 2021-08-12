package modules.mx.gui

import javafx.collections.ObservableList
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.MXUser
import modules.mx.logic.MXUserManager
import modules.mx.rightButtonsWidth
import tornadofx.*

@ExperimentalSerializationApi
class MGXUserManager : Fragment("User Management")
{
    private val userManager: MXUserManager by inject()

    @ExperimentalSerializationApi
    private val credentials = userManager.getCredentials()
    private var users: ObservableList<MXUser> = observableListOf(MXUser("", ""))
    override val root = borderpane {
        users = userManager.getUsers(users, credentials)
        center = tableview(users) {
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
            onUserSelect(1) {
                userManager.showUser(it, credentials, users)
            }
        }
        right = vbox {
            button("Add user") {
                action {
                    userManager.addUser(credentials, users)
                }
                prefWidth = rightButtonsWidth
            }
        }
    }
}
