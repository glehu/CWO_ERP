package modules.m1.gui

import kotlinx.serialization.ExperimentalSerializationApi
import modules.IOverview
import modules.m1.logic.M1Controller
import modules.m1.misc.SongModelP1
import modules.m1.misc.SongModelP2
import modules.mx.rightButtonsWidth
import tornadofx.*

@ExperimentalSerializationApi
class MG1Overview : IOverview, View("M1 Songs")
{
    private val m1Controller: M1Controller by inject()
    private val songP1: SongModelP1 by inject()
    private val songP2: SongModelP2 by inject()

    override val root = borderpane {
        right = vbox {
            button("Search") {
                action { m1Controller.openSearchScreen() }
                tooltip("Opens the search screen.")
                prefWidth = rightButtonsWidth
            }
            button("New") {
                action { m1Controller.openWizardNewSong() }
                tooltip("Add a new song to the database.")
                prefWidth = rightButtonsWidth
            }
            button("Save") {
                action { m1Controller.saveSong() }
                tooltip("Add a new song to the database.")
                prefWidth = rightButtonsWidth
            }
            button("Analytics") {
                action { m1Controller.openAnalytics() }
                tooltip("Display a chart to show the distribution of genres.")
                prefWidth = rightButtonsWidth
            }
            button("Rebuild indices") {
                //TODO: Not yet implemented
                isDisable = true
                tooltip("Rebuilds all indices in case of faulty indices.")
                prefWidth = rightButtonsWidth
            }
            button("Data Import") {
                //TODO: Not yet implemented
                isDisable = true
                //action { m1Controller.openDataImport() }
                tooltip("Import contact data from a .csv file.")
                prefWidth = rightButtonsWidth
            }
        }
        center = form {
            vbox {
                hbox(10) {
                    style {
                        backgroundColor += c("#373e43")
                        paddingAll = 10
                    }
                    fieldset("Main Data") {
                        add(SongMainData::class)
                    }
                    vbox {
                        fieldset("Album/EP Data") {
                            add(NewSongAlbumEPData::class)
                        }
                        fieldset("Visualization Data") {
                            add(NewSongVisualizationData::class)
                        }
                    }
                    vbox {
                        fieldset("Availability Data") {
                            add(SongAvailabilityData::class)
                        }
                        fieldset("Promotion Data") {
                            add(SongPromotionData::class)
                        }
                    }
                    vbox {
                        fieldset("Statistics Data") {
                            add(NewSongStatisticsData::class)
                        }
                        fieldset("Financial Data") {
                            add(SongFinancialData::class)
                        }
                    }
                    fieldset("Completion State") {
                        add(SongCompletionStateData::class)
                    }
                }
                squeezebox {
                    fold("Collaboration Data", expanded = false, closeable = false) {
                        add(NewSongCollaborationData::class)
                    }
                    fold("Copyright Data", expanded = false, closeable = false) {
                        add(NewSongCopyrightData::class)
                    }
                    fold("Miscellaneous", expanded = false, closeable = false) {
                        add(NewSongMiscData::class)
                    }
                }
            }
        }
    }
}