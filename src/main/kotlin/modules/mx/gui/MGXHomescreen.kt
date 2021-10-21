package modules.mx.gui

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import javafx.geometry.Pos
import javafx.scene.control.TabPane
import javafx.stage.Stage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1Discography.gui.MG1Overview
import modules.m1Discography.logic.M1Benchmark
import modules.m2Contacts.gui.MG2Overview
import modules.m3Invoices.gui.MG3Overview
import modules.m4Items.gui.MG4Overview
import modules.m4Items.gui.MG4PriceManager
import modules.mx.*
import modules.mx.gui.userAlerts.MGXUserAlert
import modules.mx.logic.*
import modules.mx.misc.MXUserModel
import modules.mx.misc.getUserPropertyFromUser
import styling.Stylesheet
import tornadofx.*

@DelicateCoroutinesApi
@InternalAPI
@ExperimentalSerializationApi
class CWOMainGUI : IModule, App(MXGLogin::class, Stylesheet::class) {
    override val moduleNameLong = "CWO ERP"
    override val module = "MX"
    private val userManager: MXUserManager by inject()
    override fun getIndexManager(): IIndexManager? {
        return null
    }

    override fun start(stage: Stage) {
        super.start(stage)
        stage.isMaximized = true
    }

    override fun stop() {
        try {
            exitMain()
        } finally {
            super.stop()
        }
    }
}

@DelicateCoroutinesApi
@InternalAPI
@ExperimentalSerializationApi
class MXGLogin : Fragment("CWO ERP") {
    private val loginUser = MXUserModel(getUserPropertyFromUser(MXUser("", "")))
    private val userManager: MXUserManager by inject()
    override val root = borderpane {
        top = menubar {
            menu("Menu") {
                item("Preferences").action { showPreferences() }
            }
        }
        center = form {
            checkInstallation()
            if (!isClientGlobal) setStageIcon(getIcon())
            fieldset("Login") {
                alignment = Pos.CENTER
                hbox {
                    alignment = Pos.CENTER
                    addClass(Stylesheet.fieldsetBorder)
                    vbox {
                        fieldset {
                            field("Username") { textfield(loginUser.username).required() }
                            field("Password") { passwordfield(loginUser.password).required() }
                        }
                        button("Login") {
                            enableWhen(loginUser.dirty)
                            shortcut("Enter")
                            action {
                                var validResponse = false
                                runAsyncWithProgress {
                                    if (loginUser.username.value.isNotEmpty() && loginUser.username.value.isNotEmpty()) {
                                        validResponse =
                                            userManager.login(loginUser.username.value, loginUser.password.value)
                                    }
                                } ui {
                                    if (validResponse) {
                                        startupRoutines()
                                        replaceWith<MXGUserInterface>()
                                    } else {
                                        loginUser.password.value = ""
                                        if (isClientGlobal) {
                                            //The server's login response could not be validated => security warning
                                            MGXUserAlert(
                                                message = "The server's login response could not be validated.\n\n" +
                                                        "Please notify the administrator as this poses a security thread."
                                            ).openModal()
                                        }
                                    }
                                }
                            }
                        }
                        if (!isClientGlobal) {
                            imageview { image = getLogo() }
                        }
                    }
                }
            }
        }
    }
}

@DelicateCoroutinesApi
@InternalAPI
@ExperimentalSerializationApi
class MXGUserInterface : View(titleGlobal) {
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
                    item("Insert 10k random songs").action {
                        runBlocking { M1Benchmark().insertRandomEntries(10_000) }
                    }
                    item("Insert 100k random songs").action {
                        runBlocking { M1Benchmark().insertRandomEntries(100_000) }
                    }
                    item("Insert 1m random songs").action {
                        runBlocking { M1Benchmark().insertRandomEntries(1_000_000) }
                    }
                }
            }
        }
        center = tabpane {
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

            if (activeUser.canAccessM1) tab<MG1Overview>()
            if (activeUser.canAccessM2) tab<MG2Overview>()
            if (activeUser.canAccessM3) tab<MG3Overview>()
            if (activeUser.canAccessM4) {
                tab<MG4Overview>()
                tab<MG4PriceManager>()
            }
            if (activeUser.canAccessMX) {
                //Only server can do this
                if (!isClientGlobal) tab<MGXManagement>()
            }
        }
    }
}
