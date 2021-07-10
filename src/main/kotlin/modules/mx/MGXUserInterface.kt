package modules.mx

import db.CwODB
import javafx.scene.control.TabPane
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.gui.SongController
import modules.m1.misc.M1Benchmark
import modules.m2.gui.ContactController
import tornadofx.*

@ExperimentalSerializationApi
class CWOMainGUI : App(MainView::class, Style::class)
class Style : Stylesheet()
{
    init
    {
        Companion.root {
            prefHeight = 600.px
            prefWidth = 1000.px
        }
    }
}

@ExperimentalSerializationApi
class MainView : View("CWO ERP")
{
    private val m1Controller: SongController by inject()
    private val m2Controller: ContactController by inject()

    override val root = borderpane {
        startupRoutines()
        top = menubar {
            menu("Misc") {
                menu("Log") {
                    menu("Show Logfile") {
                        item("M1 Songs").isDisable = true //Not yet implemented
                        item("M2 Contacts").isDisable = true //Not yet implemented
                    }
                    separator()
                    item("Clear Logfiles").action { MXLog.deleteLogFiles() }
                    menu("Delete Logfile") {
                        item("M1 Songs").action { MXLog.deleteLogFile("M1") }
                        item("M2 Contacts").action { MXLog.deleteLogFile("M2") }
                    }
                }
                menu("Benchmark") {
                    item("Insert entries").action { M1Benchmark().insertRandomEntries(1000000) }
                }
            }
        }
        right = vbox {
            //imageview(resources["/graphics/orochi_logo_red_400_400.png"])
        }
        center = tabpane {
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            tab("M1Songs") {
                vbox {
                    hbox {
                        //Main functions
                        vbox {
                            button("New Song") {
                                action { m1Controller.openWizardNewSong() }
                                tooltip("Add a new song to the database.")
                                vboxConstraints { marginTop = 10.0; marginLeft = 10.0 }
                            }
                            button("Find Song") {
                                action { m1Controller.openWizardFindSong() }
                                tooltip("Find a song in the database.")
                                vboxConstraints { marginTop = 10.0; marginLeft = 10.0 }
                            }
                        }
                        //Analytics functions
                        vbox {
                            button("Genre Distributions") {
                                action { m1Controller.openGenreDistribution() }
                                tooltip("Display a chart to show the distribution of genres.")
                                vboxConstraints { marginTop = 10.0; marginLeft = 10.0 }
                            }
                        }
                        //Maintenance functions
                        vbox {
                            button("Rebuild indices") {
                                tooltip("Rebuilds all indices in case of faulty indices.")
                                vboxConstraints { marginTop = 10.0; marginLeft = 40.0 }
                            }
                        }
                    }
                }
            }
            tab("M2Contacts") {
                vbox {
                    hbox {
                        //Main functions
                        vbox {
                            button("New Contact") {
                                action { m2Controller.openWizardNewContact() }
                                tooltip("Add a new song to the database.")
                                vboxConstraints { marginTop = 10.0; marginLeft = 10.0 }
                            }
                            button("Find Contact") {
                                action { m2Controller.openWizardFindContact() }
                                tooltip("Find a song in the database.")
                                vboxConstraints { marginTop = 10.0; marginLeft = 10.0 }
                            }
                        }
                        //Data import
                        vbox {
                            button("Data Import") {
                                action { m2Controller.openDataImport() }
                                tooltip("Import contact data from a .csv file.")
                                vboxConstraints { marginTop = 10.0; marginLeft = 10.0 }
                            }
                        }
                    }
                }
            }
        }
    }
}