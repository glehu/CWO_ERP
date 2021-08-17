package modules.mx.gui

import api.SpotifyAPI
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tornadofx.*

class MGXAPI : Fragment("API")
{
    override val root = borderpane {
        right = vbox {
            button("Spotify Auth") {
                action {
                    runBlocking { launch { SpotifyAPI().authorize() } }
                }
            }
        }
    }
}