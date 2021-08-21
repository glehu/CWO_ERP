package modules.mx.gui

import interfaces.IModule
import javafx.scene.control.TabPane
import javafx.stage.Stage
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.gui.MG1Overview
import modules.m1.logic.M1Benchmark
import modules.m2.gui.MG2Overview
import modules.m3.gui.MG3InvoiceFinder
import modules.mx.MXUser
import modules.mx.activeUser
import modules.mx.logic.MXLog
import modules.mx.logic.MXUserManager
import modules.mx.logic.loginRoutines
import modules.mx.misc.MXUserModel
import modules.mx.misc.getUserPropertyFromUser
import modules.mx.server
import styling.Stylesheet
import tornadofx.*

@ExperimentalSerializationApi
class CWOMainGUI : IModule, App(MXGLogin::class, Stylesheet::class)
{
    override fun moduleNameLong() = "CWO ERP"
    override fun module() = "MX"

    override fun start(stage: Stage)
    {
        super.start(stage)
        stage.isMaximized = true
    }

    override fun stop()
    {
        MXLog.log(module(), MXLog.LogType.INFO, "Shutting down server...", moduleNameLong())
        server.serverEngine.stop(100L, 100L)
        super.stop()
    }
}

@ExperimentalSerializationApi
class MXGLogin : Fragment("CWO ERP")
{
    private val loginUser = MXUserModel(getUserPropertyFromUser(MXUser("", "")))
    private val userManager: MXUserManager by inject()
    override val root = borderpane {
        top = menubar {
            menu("Menu") {
                item("Preferences").action { showPreferences() }
            }
        }
        center = form {
            loginRoutines()
            fieldset("Login Credentials") {
                addClass(Stylesheet.fieldsetBorder)
                vbox {
                    fieldset {
                        field("Username") { textfield(loginUser.username).required() }
                        field("Password") { passwordfield(loginUser.password).required() }
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
                                replaceWith<MXGUserInterface>()
                            } else loginUser.password.value = ""
                        }
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

            if (activeUser.canAccessM1) tab<MG1Overview>()
            if (activeUser.canAccessM2) tab<MG2Overview>()
            if (activeUser.canAccessM3) tab<MG3InvoiceFinder>()
            if (activeUser.canAccessMX) tab<MGXManagement>()
        }
    }
}