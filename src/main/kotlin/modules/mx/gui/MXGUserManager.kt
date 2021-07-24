package modules.mx.gui

import javafx.collections.ObservableList
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import modules.mx.MXCredentials
import modules.mx.MXUser
import modules.mx.logic.MXUserManager
import tornadofx.*

class MXGUserManager : Fragment("MX Management")
{
    private val userManager: MXUserManager by inject()
    private val credentials = userManager.getCredentials()
    private var users: ObservableList<MXUser> = observableList(MXUser("", ""))
    override val root = borderpane {
        getUsers()
        center = tableview(users) {
            readonlyColumn("Username", MXUser::username).prefWidth(200.0)
            readonlyColumn("Password (encrypted)", MXUser::password)
            readonlyColumn("MX", MXUser::canAccessMX)
                .cellFormat { text = ""; style { backgroundColor = getRightsCellColor(it) } }
            readonlyColumn("M1", MXUser::canAccessM1)
                .cellFormat { text = ""; style { backgroundColor = getRightsCellColor(it) } }
            readonlyColumn("M2", MXUser::canAccessM2)
                .cellFormat { text = ""; style { backgroundColor = getRightsCellColor(it) } }
            readonlyColumn("M3", MXUser::canAccessM3)
                .cellFormat { text = ""; style { backgroundColor = getRightsCellColor(it) } }
            onUserSelect(1) {
                showUser(it, credentials)
            }
        }
        right = vbox {
            button("Add user") {
                action {
                    addUser(credentials)
                }
                prefHeight = 50.0
            }
        }
    }

    private fun getRightsCellColor(hasRight: Boolean): MultiValue<Paint> =
        if (hasRight) MultiValue(arrayOf(Color.GREEN)) else MultiValue(arrayOf(Color.RED))

    private fun getUsers()
    {
        users.clear()
        for ((_, v) in credentials.credentials) users.add(v)
    }

    private fun addUser(credentials: MXCredentials) = showUser(MXUser("", ""), credentials)

    private fun showUser(user: MXUser, credentials: MXCredentials)
    {
        MXGUser(user, credentials).openModal(block = true)
        getUsers()
    }
}