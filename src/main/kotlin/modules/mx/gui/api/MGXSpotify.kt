package modules.mx.gui.api

import api.SpotifyAPI
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import modules.mx.rightButtonsWidth
import tornadofx.*

class MGXSpotify : View("Spotify API")
{
    private val authURLProperty = SimpleStringProperty()
    val authCodeProperty = SimpleStringProperty()
    private val accessTokenProperty = SimpleStringProperty()
    private val expiresInProperty = SimpleIntegerProperty()
    private val refreshTokenProperty = SimpleStringProperty()
    override val root = form {
        val tokenData = SpotifyAPI().getAccessAndRefreshToken()
        accessTokenProperty.value = tokenData.access_token
        expiresInProperty.value = tokenData.expires_in
        refreshTokenProperty.value = tokenData.refresh_token
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

                    }
                }
            }
            field("Access Token") {
                textfield(accessTokenProperty) {
                    prefWidth = 1500.0
                    isEditable = false
                }
            }
            field("Refresh Token") {
                textfield(expiresInProperty) {
                    prefWidth = 1500.0
                    isEditable = false
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

    fun getAccessAndRefreshToken()
    {
        val jsonResponse = SpotifyAPI().getAccessTokenFromAuthCode(authCodeProperty.value)
        accessTokenProperty.value = jsonResponse.access_token
        expiresInProperty.value = jsonResponse.expires_in
        refreshTokenProperty.value = jsonResponse.refresh_token
    }
}