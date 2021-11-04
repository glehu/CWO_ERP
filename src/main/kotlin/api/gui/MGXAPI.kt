package api.gui

import io.ktor.util.*
import javafx.scene.control.TabPane
import javafx.scene.layout.Priority
import kotlinx.serialization.ExperimentalSerializationApi
import styling.Stylesheet
import tornadofx.*


@InternalAPI
@ExperimentalSerializationApi
class MGXAPI : View("API") {
    override val root = tabpane {
        tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
        tab<MGXAPIDashboard>()
        find<MGXAPIDashboard>().getWebModuleUsageData()
        tab("Spotify") {
            vbox(10) {
                style {
                    paddingAll = 10
                }
                addClass(Stylesheet.fieldsetBorder)
                fieldset {
                    add(GSpotify::class)
                    addClass(Stylesheet.fieldsetBorder)
                    hgrow = Priority.ALWAYS
                }
            }
        }
    }
}
