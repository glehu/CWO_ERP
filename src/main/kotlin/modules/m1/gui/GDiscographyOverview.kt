package modules.m1.gui

import interfaces.IOverview
import io.ktor.util.*
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.logic.DiscographyController
import modules.mx.isClientGlobal
import modules.mx.rightButtonsWidth
import styling.Stylesheet
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class GDiscographyOverview : IOverview, View("Discography") {
    private val discographyController: DiscographyController by inject()
    override val root = borderpane {
        right = vbox {
            button("Search") {
                action { discographyController.searchEntry() }
                tooltip("Opens the search screen.")
                prefWidth = rightButtonsWidth
            }
            button("New") {
                action { discographyController.newEntry() }
                tooltip("Add a new entry to the database.")
                prefWidth = rightButtonsWidth
            }
            button("Save") {
                action { runBlocking { discographyController.saveEntry(unlock = true) } }
                tooltip("Saves the current song.")
                prefWidth = rightButtonsWidth
            }
            button("Analytics") {
                isDisable = isClientGlobal
                action { discographyController.openAnalytics() }
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
            discographyController.newEntry()
            vbox {
                hbox(10) {
                    style {
                        unsafe("-fx-background-color", Color.web("#373e43", 1.0))
                        paddingAll = 10
                    }
                    addClass(Stylesheet.fieldsetBorder)
                    fieldset("Main Data") {
                        add(SongMainData::class)
                        addClass(Stylesheet.fieldsetBorder)
                        hgrow = Priority.ALWAYS
                    }
                    vbox {
                        fieldset("Album/EP Data") {
                            add(SongAlbumEPData::class)
                        }
                        fieldset("Visualization Data") {
                            add(SongVisualizationData::class)
                        }
                        addClass(Stylesheet.fieldsetBorder)
                    }
                    vbox {
                        fieldset("Availability Data") {
                            add(SongAvailabilityData::class)
                        }
                        fieldset("Promotion Data") {
                            add(SongPromotionData::class)
                        }
                        addClass(Stylesheet.fieldsetBorder)
                    }
                    vbox {
                        fieldset("Statistics Data") {
                            add(SongStatisticsData::class)
                        }
                        fieldset("Financial Data") {
                            add(SongFinancialData::class)
                        }
                        addClass(Stylesheet.fieldsetBorder)
                    }
                    fieldset("Completion State") {
                        add(SongCompletionState::class)
                        addClass(Stylesheet.fieldsetBorder)
                    }
                }
                squeezebox {
                    fold("Collaboration Data", expanded = false, closeable = false) {
                        add(SongCollaborationData::class)
                    }
                    fold("Copyright Data", expanded = false, closeable = false) {
                        add(SongCopyrightData::class)
                    }
                    fold("Miscellaneous", expanded = true, closeable = false) {
                        add(SongMiscData::class)
                    }
                }
            }
        }
    }
}
