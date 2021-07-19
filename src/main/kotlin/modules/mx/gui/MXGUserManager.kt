package modules.mx.gui

import javafx.collections.ObservableList
import modules.m1.gui.SongViewerWizard
import modules.mx.logic.MXPasswordManager
import modules.mx.misc.MXUser
import modules.mx.misc.UserModel
import modules.mx.misc.getUserPropertyFromUser
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
            readonlyColumn("Username", MXUser::username).prefWidth(300.0)
            readonlyColumn("Password (encrypted)", MXUser::password).prefWidth(300.0)
            onUserSelect(1) {
                showUser(it)
                close()
            }
        }
    }

    private fun showUser(user: MXUser)
    {
        MXGUser(user).openModal()
    }
}