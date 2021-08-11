package modules.mx.gui

import javafx.collections.ObservableList
import javafx.scene.control.TabPane
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.logic.M1IndexManager
import modules.m2.logic.M2IndexManager
import modules.m3.logic.M3IndexManager
import modules.mx.*
import modules.mx.logic.MXLog
import modules.mx.logic.MXUserManager
import tornadofx.*
import java.io.File

class MGXManagement : Fragment("MX Management")
{
    private val userManager: MXUserManager by inject()

    @ExperimentalSerializationApi
    private val credentials = userManager.getCredentials()
    private var users: ObservableList<MXUser> = observableListOf(MXUser("", ""))

    @ExperimentalSerializationApi
    override val root = tabpane {
        tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
        //----------------------------------v
        //-------- User Management ---------|
        //----------------------------------^
        tab("Users") {
            borderpane {
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
                        prefWidth = rightButtonsWidth
                    }
                }
            }
        }
        //----------------------------------v
        //-------- Databases Management ----|
        //----------------------------------^
        tab("Databases") {
            form {
                fieldset("Reset Database") {
                    button("M1 Songs") {
                        action {
                            if (File(getModulePath("M1")).isDirectory)
                            {
                                File(getModulePath("M1")).deleteRecursively()
                            }
                            MXLog.checkLogFile("M1", true)
                            m1GlobalIndex = M1IndexManager()
                        }
                        prefWidth = rightButtonsWidth * 1.5
                        textFill = Color.RED
                    }
                    button("M2 Contacts") {
                        action {
                            if (File(getModulePath("M2")).isDirectory)
                            {
                                File(getModulePath("M2")).deleteRecursively()
                            }
                            MXLog.checkLogFile("M2", true)
                            m2GlobalIndex = M2IndexManager()
                        }
                        prefWidth = rightButtonsWidth * 1.5
                        textFill = Color.RED
                    }
                    button("M3 Invoices") {
                        action {
                            if (File(getModulePath("M3")).isDirectory)
                            {
                                File(getModulePath("M3")).deleteRecursively()
                            }
                            MXLog.checkLogFile("M3", true)
                            m3GlobalIndex = M3IndexManager()
                        }
                        prefWidth = rightButtonsWidth * 1.5
                        textFill = Color.RED
                    }
                }
            }
        }
    }

    private fun getRightsCellColor(hasRight: Boolean): MultiValue<Paint> =
        if (hasRight) MultiValue(arrayOf(Color.GREEN)) else MultiValue(arrayOf(Color.RED))

    @ExperimentalSerializationApi
    private fun getUsers()
    {
        users.clear()
        for ((_, v) in credentials.credentials) users.add(v)
    }

    @ExperimentalSerializationApi
    private fun addUser(credentials: MXCredentials) = showUser(MXUser("", ""), credentials)

    @ExperimentalSerializationApi
    private fun showUser(user: MXUser, credentials: MXCredentials)
    {
        MGXUser(user, credentials).openModal(block = true)
        getUsers()
    }
}