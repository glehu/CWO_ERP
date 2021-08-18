package modules.mx.gui

import Styling.Stylesheet
import javafx.scene.layout.Priority
import modules.m1.gui.SongMainData
import modules.mx.gui.api.MGXSpotify
import tornadofx.*


class MGXAPI : View("API")
{
    override val root = borderpane {
        center = vbox(10) {
            style {
                paddingAll = 10
            }
            addClass(Stylesheet.fieldsetBorder)
            fieldset("Spotify") {
                add(MGXSpotify::class)
                addClass(Stylesheet.fieldsetBorder)
                hgrow = Priority.ALWAYS
            }
        }
    }
}