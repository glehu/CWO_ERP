package modules.api.gui

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import modules.api.json.SpotifyAuthCallbackJson
import modules.api.json.SpotifyUserProfileJson
import modules.api.logic.SpotifyAPI
import modules.api.logic.SpotifyAUTH
import modules.api.logic.SpotifyController
import modules.mx.rightButtonsWidth
import styling.Stylesheet.Companion.fieldsetBorder
import tornadofx.*

class GSpotify : View("Spotify API")
{
    private val sAUTH = SpotifyAUTH()
    private val sAPI = SpotifyAPI()
    private val controller: SpotifyController by inject()

    //Account Data
    private val accountNameProperty = SimpleStringProperty()
    private val accountTypeProperty = SimpleStringProperty()
    private val accountProductProperty = SimpleStringProperty()
    private val accountIDProperty = SimpleStringProperty()
    private val accountFollowersProperty = SimpleStringProperty()

    //API Dev Test
    private val artistSpotifyIDProperty = SimpleStringProperty()

    //Token Data
    private val authURLProperty = SimpleStringProperty()
    val authCodeProperty = SimpleStringProperty()
    private val accessTokenProperty = SimpleStringProperty()
    private val generatedAtUnixTimestampProperty = SimpleStringProperty()
    private val expiresInProperty = SimpleIntegerProperty()
    private val expireUnixTimestampProperty = SimpleStringProperty()
    private val refreshTokenProperty = SimpleStringProperty()

    override val root = form {
        showTokenData(sAUTH.getAccessAndRefreshTokenFromDisk() as SpotifyAuthCallbackJson)
        showUserData(controller.getUserData())
        authURLProperty.value = sAUTH.getAuthorizationURL()
        squeezebox {
            fold("Spotify Account Data", expanded = true, closeable = false) {
                form {
                    fieldset {
                        addClass(fieldsetBorder)
                        button("Get Data") {
                            prefWidth = rightButtonsWidth
                            action {
                                updateUserData()
                            }
                            isDisable = (accessTokenProperty.value.isEmpty())
                        }
                        hbox(10) {
                            field("Account Name") {
                                textfield(accountNameProperty) { isEditable = false }
                            }
                            field("Account Type") {
                                textfield(accountTypeProperty) { isEditable = false }
                            }
                            field("Account Product") {
                                textfield(accountProductProperty) { isEditable = false }
                            }
                        }
                        field("Spotify ID") {
                            textfield(accountIDProperty) { isEditable = false }
                        }
                        field("Followers") {
                            textfield(accountFollowersProperty) { isEditable = false }
                        }
                    }
                }
            }
            fold("API Dev Test", expanded = true, closeable = false) {
                form {
                    fieldset {
                        addClass(fieldsetBorder)
                        field("Artist SpotifyID") { textfield(artistSpotifyIDProperty) }
                        button("Get Artist Album List") {
                            action {
                                sAPI.getArtistAlbumList(artistSpotifyIDProperty.value)
                            }
                        }
                    }
                }
            }
            fold("Spotify Token Data", expanded = false, closeable = false) {
                form {
                    fieldset {
                        addClass(fieldsetBorder)
                        button("Refresh Token") {
                            prefWidth = rightButtonsWidth
                            action {
                                sAUTH.refreshAccessToken()
                                showTokenData(
                                    sAUTH.getAccessAndRefreshTokenFromDisk() as SpotifyAuthCallbackJson
                                )
                            }
                        }
                        field("Authorization URL") {
                            textfield(authURLProperty) {
                                isEditable = false
                                tooltip("Enter this URL in your browser to authorize CWO ERP.")
                            }
                        }
                        field("Authorization Code") {
                            textfield(authCodeProperty) { isEditable = false }
                        }
                        field("Access Token") {
                            textfield(accessTokenProperty) { isEditable = false }
                        }
                        hbox(10) {
                            field("Generated at (UNIXTIME)") {
                                textfield(generatedAtUnixTimestampProperty) { isEditable = false }
                            }
                            field("Expires in (seconds)") {
                                textfield(expiresInProperty) { isEditable = false }
                            }
                            field("at (UNIXTIME)") {
                                textfield(expireUnixTimestampProperty) { isEditable = false }
                            }
                        }
                        field("Refresh Token") {
                            textfield(refreshTokenProperty) { isEditable = false }
                        }
                    }
                }
            }
        }
    }

    private fun updateUserData()
    {
        val userData = sAPI.getAccountData()
        showUserData(userData)
    }

    fun showTokenData(tokenData: SpotifyAuthCallbackJson)
    {
        accessTokenProperty.value = tokenData.accessToken
        generatedAtUnixTimestampProperty.value = tokenData.generatedAtUnixTimestamp.toString()
        expiresInProperty.value = tokenData.expiresIn
        expireUnixTimestampProperty.value = tokenData.expireUnixTimestamp.toString()
        refreshTokenProperty.value = tokenData.refreshToken
    }

    private fun showUserData(userData: SpotifyUserProfileJson)
    {
        accountNameProperty.value = userData.display_name
        accountTypeProperty.value = userData.type
        accountProductProperty.value = userData.product
        accountIDProperty.value = userData.id
        if (userData.followers["total"] != null)
        {
            accountFollowersProperty.value = userData.followers["total"]
        }
    }
}