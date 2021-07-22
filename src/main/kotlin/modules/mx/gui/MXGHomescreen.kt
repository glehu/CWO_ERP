package modules.mx.gui

import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.TabPane
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.gui.MG1SongFinder
import modules.m1.logic.M1Controller
import modules.m1.misc.M1Benchmark
import modules.m2.gui.MG2ContactFinder
import modules.m2.logic.M2Controller
import modules.m3.gui.MG3InvoiceFinder
import modules.m3.logic.M3Controller
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
            prefHeight = 700.px
            prefWidth = 1300.px
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
                field("Username") { textfield(usernameProperty) }
                field("Password") { passwordfield(passwordProperty) }
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
}

@ExperimentalSerializationApi
class MXGUserInterface : View("CWO ERP")
{
    private val m1Controller: M1Controller by inject()
    private val m2Controller: M2Controller by inject()
    private val m3Controller: M3Controller by inject()

    private val buttonWidth = 150.0

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
            if (activeUser.canAccessM1) tab("M1Songs") {
                borderpane {
                    center {
                        add<MG1SongFinder>()
                    }
                    right {
                        vbox {
                            //Main functions
                            button("New Song") {
                                action { m1Controller.openWizardNewSong() }
                                tooltip("Add a new song to the database.")
                                prefWidth = buttonWidth
                            }
                            button("Find Song") {
                                action { m1Controller.openWizardFindSong() }
                                tooltip("Find a song in the database.")
                                prefWidth = buttonWidth
                            }
                            //Analytics functions
                            button("Analytics") {
                                action { m1Controller.openAnalytics() }
                                tooltip("Display a chart to show the distribution of genres.")
                                prefWidth = buttonWidth
                            }
                            //Maintenance functions
                            button("Rebuild indices") {
                                //TODO: Not yet implemented
                                isDisable = true
                                tooltip("Rebuilds all indices in case of faulty indices.")
                                prefWidth = buttonWidth
                            }
                            //Data import
                            button("Data Import") {
                                //TODO: Not yet implemented
                                isDisable = true
                                //action { m1Controller.openDataImport() }
                                tooltip("Import contact data from a .csv file.")
                                prefWidth = buttonWidth
                            }
                        }
                    }
                }
            }
            if (activeUser.canAccessM2) tab("M2Contacts") {
                borderpane {
                    center {
                        add<MG2ContactFinder>()
                    }
                    right {
                        vbox {
                            //Main functions
                            button("New Contact") {
                                action { m2Controller.openWizardNewContact() }
                                tooltip("Add a new contact to the database.")
                                prefWidth = buttonWidth
                            }
                            /*
                        button("Find Contact") {
                            action { m2Controller.openWizardFindContact() }
                            tooltip("Find a contact in the database.")
                            vboxConstraints { marginTop = 10.0; marginLeft = 10.0 }
                            prefWidth = buttonWidth
                        }
                         */
                            //Analytics functions
                            button("Analytics") {
                                action { m2Controller.openAnalytics() }
                                tooltip("Display a chart to show the distribution of genres.")
                                prefWidth = buttonWidth
                            }
                            //Maintenance functions
                            button("Rebuild indices") {
                                //TODO: Not yet implemented
                                isDisable = true
                                tooltip("Rebuilds all indices in case of faulty indices.")
                                prefWidth = buttonWidth
                            }
                            //Data import
                            button("Data Import") {
                                action { m2Controller.openDataImport() }
                                tooltip("Import contact data from a .csv file.")
                                prefWidth = buttonWidth
                            }
                        }
                    }
                    if (activeUser.canAccessM3) tab("M3Invoices")
                    {
                        borderpane {
                            center {
                                add<MG3InvoiceFinder>()
                            }
                            right {
                                //Main functions
                                vbox {
                                    button("New Invoice") {
                                        action { m3Controller.openWizardNewInvoice() }
                                        tooltip("Add a new song to the database.")
                                        prefWidth = buttonWidth
                                    }
                                    /*
                                    button("Find Invoice") {
                                        action { m3Controller.openWizardFindInvoice() }
                                        tooltip("Find a song in the database.")
                                    }
                                     */
                                    //Analytics functions
                                    button("Analytics") {
                                        //action { m3Controller.openAnalytics() }
                                        tooltip("Display a chart to show the distribution of genres.")
                                        prefWidth = buttonWidth
                                    }
                                    //Maintenance functions
                                    button("Rebuild indices") {
                                        //TODO: Not yet implemented
                                        isDisable = true
                                        tooltip("Rebuilds all indices in case of faulty indices.")
                                        prefWidth = buttonWidth
                                    }
                                    //Data import
                                    button("Data Import") {
                                        //TODO: Not yet implemented
                                        isDisable = true
                                        //action { m3Controller.openDataImport() }
                                        tooltip("Import contact data from a .csv file.")
                                        prefWidth = buttonWidth
                                    }
                                }
                            }
                            if (activeUser.canAccessMX) tab("MX") { add<MXGUserManager>() }
                        }
                    }
                }
            }
        }
    }
}