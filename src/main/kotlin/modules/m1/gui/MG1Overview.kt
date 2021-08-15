package modules.m1.gui

import kotlinx.serialization.ExperimentalSerializationApi
import modules.IOverview
import modules.m1.logic.M1Controller
import modules.mx.rightButtonsWidth
import tornadofx.*

@ExperimentalSerializationApi
class MG1Overview : IOverview, View("M1 Songs")
{
    private val m1Controller: M1Controller by inject()
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
    }
}