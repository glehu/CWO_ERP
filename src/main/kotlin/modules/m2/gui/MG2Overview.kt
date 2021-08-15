package modules.m2.gui

import kotlinx.serialization.ExperimentalSerializationApi
import modules.IOverview
import modules.m1.gui.SongMainData
import modules.m2.logic.M2Controller
import modules.mx.rightButtonsWidth
import tornadofx.*

@ExperimentalSerializationApi
class MG2Overview : IOverview, View("M2 Contacts")
{
    private val m2Controller: M2Controller by inject()
    override val root = borderpane {
        right = vbox {
            button("Search") {
                action { m2Controller.openSearchScreen() }
                tooltip("Opens the search screen.")
                prefWidth = rightButtonsWidth
            }
            button("New") {
                action { m2Controller.openWizardNewContact() }
                tooltip("Add a new song to the database.")
                prefWidth = rightButtonsWidth
            }
            button("Save") {
                action { m2Controller.saveContact() }
                tooltip("Add a new song to the database.")
                prefWidth = rightButtonsWidth
            }
            //Analytics functions
            button("Analytics") {
                action { m2Controller.openAnalytics() }
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
                action { m2Controller.openDataImport() }
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
                        add(NewContactMainData::class)
                    }
                }
            }
        }
    }
}