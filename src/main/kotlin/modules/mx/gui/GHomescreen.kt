package modules.mx.gui

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Pos
import javafx.scene.control.TabPane
import javafx.scene.paint.Color
import javafx.stage.Stage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.gui.GDiscographyOverview
import modules.m1.logic.DiscographyBenchmark
import modules.m2.gui.GContactOverview
import modules.m3.gui.GInvoiceOverview
import modules.m4.gui.GItemOverview
import modules.m4.gui.GItemPriceManager
import modules.m4.gui.GItemStorageManager
import modules.mx.*
import modules.mx.gui.userAlerts.GAlert
import modules.mx.logic.*
import modules.mx.misc.UserModel
import modules.mx.misc.getUserPropertyFromUser
import styling.Stylesheet
import tornadofx.*

@DelicateCoroutinesApi
@InternalAPI
@ExperimentalSerializationApi
class CWOMainGUI : IModule, App(GLogin::class, Stylesheet::class) {
    override val moduleNameLong = "CWO ERP"
    override val module = "MX"
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
class GLogin : Fragment("CWO ERP") {
    private val loginUser = UserModel(getUserPropertyFromUser(User("", "")))
    private val modeOffline = SimpleBooleanProperty(false)
    private val modeSafety = SimpleBooleanProperty(false)
    private val userManager: UserManager by inject()
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
                        hbox(50) {
                            fieldset("Credentials") {
                                field("Username") { textfield(loginUser.username).required() }
                                field("Password") { passwordfield(loginUser.password).required() }
                            }
                            fieldset("Options") {
                                field("Offline (F1)") {
                                    shortcut("F1") {
                                        if (!modeSafety.value) {
                                            modeOffline.value = !modeOffline.value
                                        }
                                    }
                                    checkbox("", modeOffline) {
                                        tooltip("Starts the software without its network components.")
                                        action {
                                            if (modeSafety.value) {
                                                modeOffline.value = !modeOffline.value
                                            }
                                        }
                                    }
                                }
                                field("Safety Mode (F2)") {
                                    shortcut("F2") {
                                        modeSafety.value = !modeSafety.value
                                        modeOffline.value = modeSafety.value
                                    }
                                    checkbox("", modeSafety) {
                                        tooltip("Starts the software in offline mode without loading the indices.")
                                        action {
                                            modeOffline.value = modeSafety.value
                                        }
                                    }
                                }
                            }
                        }
                        button("Login (Enter)") {
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
                                        startupRoutines(modeOffline.value, modeSafety.value)
                                        replaceWith<GHomescreen>()
                                    } else {
                                        loginUser.password.value = ""
                                        if (isClientGlobal) {
                                            //The server's login response could not be validated => security warning
                                            GAlert(
                                                message = "The server's login response could not be validated.\n\n" +
                                                        "Please notify the administrator as this poses a security threat."
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
class GHomescreen : View(titleGlobal) {
    override val root = borderpane {
        top = menubar {
            menu("Menu") {
                item("Preferences").action { showPreferences() }
            }
            menu("Misc") {
                menu("Log") {
                    menu("Show Logfile") {
                        item("M1 Songs").action {
                            GLog("M1 Songs").showLog(Log.getLogFile("M1"), ".*".toRegex())
                        }
                        item("M2 Contacts").action {
                            GLog("M2 Contacts").showLog(Log.getLogFile("M2"), ".*".toRegex())
                        }
                        item("M3 Invoice").action {
                            GLog("M3 Invoice").showLog(Log.getLogFile("M3"), ".*".toRegex())
                        }
                        item("M4 Item").action {
                            GLog("M4 Item").showLog(Log.getLogFile("M4"), ".*".toRegex())
                        }
                        item("MX").action {
                            GLog("MX").showLog(Log.getLogFile("MX"), ".*".toRegex())
                        }
                    }
                    separator()
                    item("Clear Logfiles").action { Log.deleteLogFiles() }
                    menu("Delete Logfile") {
                        item("M1 Songs").action { Log.deleteLogFile("M1") }
                        item("M2 Contacts").action { Log.deleteLogFile("M2") }
                        item("M3 Invoice").action { Log.deleteLogFile("M3") }
                        item("M4 Item").action { Log.deleteLogFile("M4") }
                        item("MX").action { Log.deleteLogFile("MX") }
                    }
                }
            }
            menu("Dev-Tools") {
                menu("Benchmark (M1)") {
                    item("Insert 10k random songs").action {
                        runBlocking { DiscographyBenchmark().insertRandomEntries(10_000) }
                    }
                    item("Insert 100k random songs").action {
                        runBlocking { DiscographyBenchmark().insertRandomEntries(100_000) }
                    }
                    item("Insert 1m random songs").action {
                        runBlocking { DiscographyBenchmark().insertRandomEntries(1_000_000) }
                    }
                }
            }
        }
        center = tabpane {
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

            if (activeUser.canAccessDiscography) tab<GDiscographyOverview>()
            if (activeUser.canAccessContacts) tab<GContactOverview>()
            if (activeUser.canAccessInvoices) tab<GInvoiceOverview>()
            if (activeUser.canAccessInventory) {
                tab<GItemOverview>()
                tab<GItemPriceManager>()
                tab<GItemStorageManager>()
            }
            if (activeUser.canAccessManagement) {
                //Only server can do this
                if (!isClientGlobal) tab<GManagement>()
            }
        }
        bottom = hbox(20) {
            if (serverJobGlobal == null) label("SERVER OFF")
            if (telnetServerJobGlobal == null) label("TELNET OFF")
            if (discographyIndexManager == null) label("M1 OFF")
            if (contactIndexManager == null) label("M2 OFF")
            if (invoiceIndexManager == null) label("M3 OFF")
            if (itemIndexManager == null) label("M4 OFF")
            if (itemStockPostingIndexManager == null) label("M4SP OFF")
            style {
                unsafe("-fx-background-color", Color.web("#373e43", 1.0))
            }
            addClass(Stylesheet.fieldsetBorder)
        }
    }
}