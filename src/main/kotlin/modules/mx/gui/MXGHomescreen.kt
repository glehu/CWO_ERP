package modules.mx.gui

import db.CwODB
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.TabPane
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.gui.MG1SongFinder
import modules.m1.logic.M1IndexManager
import modules.m1.misc.M1Benchmark
import modules.m2.gui.MG2ContactFinder
import modules.m2.logic.M2IndexManager
import modules.m3.gui.MG3InvoiceFinder
import modules.m3.logic.M3IndexManager
import modules.mx.logic.MXLog
import modules.mx.logic.MXUserManager
import modules.mx.logic.activeUser
import modules.mx.loginRoutines
import tornadofx.*

@ExperimentalSerializationApi
class CWOMainGUI : App(MXGLogin::class, StyleMain::class)
class StyleMain : Stylesheet()
{
    init
    {
        Companion.root {
            prefHeight = 900.px
            prefWidth = 1800.px
        }
    }
}

@ExperimentalSerializationApi
class MXGLogin : View("Login")
{
    private val userManager: MXUserManager by inject()
    private val usernameProperty = SimpleStringProperty()
    private val passwordProperty = SimpleStringProperty()
    override val root = form {
        loginRoutines()
        vbox {
            fieldset {
                field("Username") {
                    textfield(usernameProperty) {
                        prefWidth = 100.0
                    }
                }
                field("Password")
                {
                    passwordfield(passwordProperty) {
                        prefWidth = 100.0
                    }
                }
            }
        }
        button("Login") {
            shortcut("Enter")
            action {
                var loginSuccess = false
                runAsyncWithProgress {
                    if (usernameProperty.value != null && passwordProperty.value != null)
                    {
                        loginSuccess = userManager.login(usernameProperty.value, passwordProperty.value)
                    }
                } ui {
                    if (loginSuccess)
                    {
                        close()
                        find(MXGUserInterface::class).openModal()
                    } else passwordProperty.value = ""
                }
            }
        }
    }
}

@ExperimentalSerializationApi
class MXGUserInterface : View("CWO ERP")
{
    val db: CwODB by inject()
    private val m1IndexManager: M1IndexManager by inject(Scope(db))
    private val m2IndexManager: M2IndexManager by inject(Scope(db))
    private val m3IndexManager: M3IndexManager by inject(Scope(db))
    private val indexScope = Scope()
    override val root = borderpane {
        top = menubar {
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

            setInScope(m1IndexManager, indexScope)
            setInScope(m2IndexManager, indexScope)
            setInScope(m3IndexManager, indexScope)
            if (activeUser.canAccessM1) tab<MG1SongFinder>(indexScope)
            if (activeUser.canAccessM2) tab<MG2ContactFinder>(indexScope)
            if (activeUser.canAccessM3) tab<MG3InvoiceFinder>(indexScope)
            if (activeUser.canAccessMX) tab<MXGUserManager>()
        }
    }
}