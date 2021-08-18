package modules.mx.gui.api

import api.SpotifyAPI
import javafx.beans.property.SimpleStringProperty
import modules.mx.rightButtonsWidth
import tornadofx.*

class MGXSpotify : View("Spotify API")
{
    private val authURLProperty = SimpleStringProperty()
    val authCodeProperty = SimpleStringProperty()
    val responseProperty = SimpleStringProperty()
    override val root = form {
        fieldset {
            hbox {
                field("Authorization URL") {
                    textfield(authURLProperty) {
                        prefWidth = 1500.0
                        isEditable = false
                    }
                }
                button("Generate") {
                    prefWidth = rightButtonsWidth
                    action {
                        authURLProperty.value = SpotifyAPI().getAuthorizationURL()
                    }
                }
            }
            hbox {
                field("Authorization Code") {
                    textfield(authCodeProperty) {
                        prefWidth = 1500.0
                        isEditable = false
                    }
                }
                button("Submit") {
                    prefWidth = rightButtonsWidth
                    action {
                        responseProperty.value = SpotifyAPI().getAccessTokenFromAuthCode(authCodeProperty.value)
                    }
                }
            }
            field("Response") {
                textfield(responseProperty) {
                    prefWidth = 1500.0
                    isEditable = false
                }
            }
        }
    }
}