package modules.mx.gui.api

import api.SpotifyAPI
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import modules.mx.rightButtonsWidth
import server.SpotifyAuthCallbackJson
import tornadofx.*

class MGXSpotify : View("Spotify API")
{
    private val authURLProperty = SimpleStringProperty()
    val authCodeProperty = SimpleStringProperty()
    private val accessTokenProperty = SimpleStringProperty()
    private val generatedAtUnixTimestampProperty = SimpleLongProperty()
    private val expiresInProperty = SimpleIntegerProperty()
    private val expireUnixTimestampProperty = SimpleLongProperty()
    private val refreshTokenProperty = SimpleStringProperty()

    override val root = form {
        showTokenData(SpotifyAPI().getAccessAndRefreshTokenFromDisk())
        fieldset("Spotify Token Data") {
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
            }
            field("Access Token") {
                textfield(accessTokenProperty) {
                    prefWidth = 1500.0
                    isEditable = false
                }
            }
            hbox {
                field("Generated at (UNIXTIME)") {
                    textfield(generatedAtUnixTimestampProperty) {
                        prefWidth = 1500.0
                        isEditable = false
                    }
                }
                field("Expires in (seconds)") {
                    textfield(expiresInProperty) {
                        prefWidth = 1500.0
                        isEditable = false
                    }
                }
                field("at (UNIXTIME)") {
                    textfield(expireUnixTimestampProperty) {
                        prefWidth = 1500.0
                        isEditable = false
                    }
                }
            }
            field("Refresh Token") {
                textfield(refreshTokenProperty) {
                    prefWidth = 1500.0
                    isEditable = false
                }
            }
        }
    }

    fun showTokenData(tokenData: SpotifyAuthCallbackJson)
    {
        accessTokenProperty.value = tokenData.access_token
        generatedAtUnixTimestampProperty.value = tokenData.generatedAtUnixTimestamp
        expiresInProperty.value = tokenData.expires_in
        expireUnixTimestampProperty.value = tokenData.expireUnixTimestamp
        refreshTokenProperty.value = tokenData.refresh_token
    }
}