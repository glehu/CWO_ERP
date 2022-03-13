package api.gui

import io.ktor.util.*
import javafx.scene.control.TabPane
import javafx.scene.layout.Priority
import kotlinx.serialization.ExperimentalSerializationApi
import styling.Stylesheet
import tornadofx.View
import tornadofx.addClass
import tornadofx.fieldset
import tornadofx.hgrow
import tornadofx.paddingAll
import tornadofx.style
import tornadofx.tab
import tornadofx.tabpane
import tornadofx.vbox

@InternalAPI
@ExperimentalSerializationApi
class GAPI : View("API") {
  override val root = tabpane {
    tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
    tab<GAPIDashboard>()
    find<GAPIDashboard>().getWebModuleUsageData()
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
