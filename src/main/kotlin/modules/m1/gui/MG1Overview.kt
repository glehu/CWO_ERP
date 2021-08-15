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
                prefWidth = rightButtonsWidth
            }
            button("New Song") {
                action {
                    m1Controller.openWizardNewSong()
                }
                tooltip("Add a new song to the database.")
                prefWidth = rightButtonsWidth
            }
            //Analytics functions
            button("Analytics") {
                action { m1Controller.openAnalytics() }
                tooltip("Display a chart to show the distribution of genres.")
                prefWidth = rightButtonsWidth
            }
            //Maintenance functions
            button("Rebuild indices") {
                //TODO: Not yet implemented
                isDisable = true
                tooltip("Rebuilds all indices in case of faulty indices.")
                prefWidth = rightButtonsWidth
            }
            //Data import
            button("Data Import") {
                //TODO: Not yet implemented
                isDisable = true
                //action { m1Controller.openDataImport() }
                tooltip("Import contact data from a .csv file.")
                prefWidth = rightButtonsWidth
            }
        }
        center = form {
            vbox(10) {
                hbox(10) {
                    fieldset("Main Data") {
                        add(SongMainData::class)
                        style {
                            paddingAll = 10
                            backgroundColor += c("#373e43")
                        }
                    }
                    fieldset("Completion State") {
                        add(SongCompletionStateData::class)
                        style {
                            paddingAll = 10
                            backgroundColor += c("#373e43")
                        }
                    }
                    vbox {
                        fieldset("Availability Data") {
                            add(SongAvailabilityData::class)
                            style {
                                paddingAll = 10
                                backgroundColor += c("#373e43")
                            }
                        }
                        fieldset("Promotion Data") {
                            add(SongPromotionData::class)
                            style {
                                paddingAll = 10
                                backgroundColor += c("#373e43")
                            }
                        }
                    }
                    vbox {
                        fieldset("Financial Data") {
                            add(SongFinancialData::class)
                            style {
                                paddingAll = 10
                                backgroundColor += c("#373e43")
                            }
                        }
                        fieldset("Visualization Data") {
                            add(NewSongVisualizationData::class)
                            style {
                                paddingAll = 10
                                backgroundColor += c("#373e43")
                            }
                        }
                    }
                    fieldset("Album/EP Data") {
                        add(NewSongAlbumEPData::class)
                        style {
                            paddingAll = 10
                            backgroundColor += c("#373e43")
                        }
                    }
                    fieldset("Statistics Data") {
                        add(NewSongStatisticsData::class)
                        style {
                            paddingAll = 10
                            backgroundColor += c("#373e43")
                        }
                    }
                }
                hbox(10) {
                    fieldset("Collaboration Data") {
                        add(NewSongCollaborationData::class)
                        style {
                            paddingAll = 10
                            backgroundColor += c("#373e43")
                        }
                    }
                    fieldset("Copyright Data") {
                        add(NewSongVisualizationData::class)
                        style {
                            paddingAll = 10
                            backgroundColor += c("#373e43")
                        }
                    }
                    fieldset("Miscellaneous") {
                        add(NewSongMiscData::class)
                        style {
                            paddingAll = 10
                            backgroundColor += c("#373e43")
                        }
                    }
                }
            }
        }
    }
}