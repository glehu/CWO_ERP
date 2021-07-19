package modules.mx.gui

import javafx.collections.ObservableList
import modules.mx.logic.MXPasswordManager
import modules.mx.misc.MXUser
import tornadofx.*

class MXGUserManager : Fragment("User Management")
{
    private val passwordManager: MXPasswordManager by inject()
    private val credentials = passwordManager.getCredentials()
    private var users: ObservableList<MXUser> = observableList(MXUser("", ""))
    override val root = form {
        users.clear()
        for ((_, v) in credentials.credentials)
        {
            users.add(v)
        }
        tableview(users) {
            readonlyColumn("ID", MXUser::username).prefWidth(300.0)
            readonlyColumn("Name", MXUser::password).prefWidth(300.0)
            onUserSelect(1) {
                close()
            }
        }
    }
}