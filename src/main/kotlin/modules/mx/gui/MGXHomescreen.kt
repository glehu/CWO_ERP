package modules.mx.gui

import javafx.scene.control.TabPane
import kotlinx.serialization.ExperimentalSerializationApi
import loginRoutines
import modules.m1.gui.MG1SongFinder
import modules.m1.logic.M1Benchmark
import modules.m2.gui.MG2ContactFinder
import modules.m3.gui.MG3InvoiceFinder
import modules.mx.MXUser
import modules.mx.activeUser
import modules.mx.logic.MXLog
import modules.mx.logic.MXUserManager
import modules.mx.misc.MXUserModel
import modules.mx.misc.getUserPropertyFromUser
import tornadofx.*

@ExperimentalSerializationApi
class CWOMainGUI : App(MXGLogin::class)

@ExperimentalSerializationApi
class MXGLogin : View("CWO ERP")
{
    private val loginUser = MXUserModel(getUserPropertyFromUser(MXUser("", "")))
    private val userManager: MXUserManager by inject()
    override val root = borderpane {
        setPrefSize(350.0, 250.0)
        top = menubar {
            menu("Menu") {
                item("Preferences").action { showPreferences() }
            }
        }
        center = form {
            loginRoutines()
            vbox {
                fieldset {
                    field("Username") { textfield(loginUser.username) { prefWidth = 200.0 }.required() }
                    field("Password") { passwordfield(loginUser.password) { prefWidth = 200.0 }.required() }
                }
            }
            button("Login") {
                enableWhen(loginUser.dirty)
                shortcut("Enter")
                action {
                    var loginSuccess = false
                    runAsyncWithProgress {
                        if (loginUser.username.value.isNotEmpty() && loginUser.username.value.isNotEmpty())
                        {
                            loginSuccess = userManager.login(loginUser.username.value, loginUser.password.value)
                        }
                    } ui {
                        if (loginSuccess)
                        {
                            close()
                            find(MXGUserInterface::class).openModal()
                        } else loginUser.password.value = ""
                    }
                }
            }
        }
    }
}

@ExperimentalSerializationApi
class MXGUserInterface : View("CWO ERP")
{
    override val root = borderpane {
        top = menubar {
            menu("Menu") {
                item("Preferences").action { showPreferences() }
            }
            menu("Misc") {
                menu("Log") {
                    menu("Show Logfile") {
                        item("M1 Songs").isDisable = true //Not yet implemented
                        item("M2 Contacts").isDisable = true //Not yet implemented
                        item("M3 Invoice").isDisable = true //Not yet implemented
                    }
                    separator()
                    item("Clear Logfiles").action { MXLog.deleteLogFiles() }
                    menu("Delete Logfile") {
                        item("M1 Songs").action { MXLog.deleteLogFile("M1") }
                        item("M2 Contacts").action { MXLog.deleteLogFile("M2") }
                        item("M3 Invoice").action { MXLog.deleteLogFile("M3") }
                        item("MX").action { MXLog.deleteLogFile("MX") }
                    }
                }
            }
            menu("Dev-Tools") {
                menu("Benchmark (M1)") {
                    item("Insert random songs").action { M1Benchmark().insertRandomEntries(1000000) }
                }
            }
        }
        center = tabpane {
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

            if (activeUser.canAccessM1) tab<MG1SongFinder>()
            if (activeUser.canAccessM2) tab<MG2ContactFinder>()
            if (activeUser.canAccessM3) tab<MG3InvoiceFinder>()
            if (activeUser.canAccessMX) tab<MGXManagement>()
        }
    }
}