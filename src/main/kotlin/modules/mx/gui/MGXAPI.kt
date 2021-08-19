package modules.mx.gui

import styling.Stylesheet
import javafx.scene.layout.Priority
import modules.api.gui.GSpotify
import tornadofx.*


class MGXAPI : View("API")
{
    override val root = borderpane {
        center = vbox(10) {
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