package modules.mx.gui

import javafx.collections.ObservableList
import modules.mx.logic.MXUserManager
import modules.mx.misc.MXUser
import tornadofx.*

class MXGUserManager : Fragment("User Management")
{
    private val userManager: MXUserManager by inject()
    private val credentials = userManager.getCredentials()
    private var users: ObservableList<MXUser> = observableList(MXUser("", ""))
    override val root = form {
        getUsers()
        button("Add user") {
            action {
                addUser(MXUser("",""))
            }
        }
        tableview(users) {
            readonlyColumn("Username", MXUser::username).prefWidth(200.0)
            readonlyColumn("Password (encrypted)", MXUser::password)
            readonlyColumn("Access M1", MXUser::canAccessM1Song)
            readonlyColumn("Access M2", MXUser::canAccessM2Contact)
            onUserSelect(1) {
                showUser(it)
                close()
            }
        }
    }

    private fun addUser(user: MXUser)
    {
        showUser(user)
        close()
    }

    private fun getUsers()
    {
        users.clear()
        for ((_, v) in credentials.credentials)
        {
            users.add(v)
        }
    }

    private fun showUser(user: MXUser)
    {
        MXGUser(user).openModal()
    }
}