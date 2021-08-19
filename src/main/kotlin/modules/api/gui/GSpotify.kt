package modules.api.gui

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import modules.api.json.SpotifyAuthCallbackJson
import modules.api.logic.SpotifyAPI
import modules.api.logic.SpotifyAUTH
import modules.mx.rightButtonsWidth
import styling.Stylesheet.Companion.fieldsetBorder
import tornadofx.*

class GSpotify : View("Spotify API")
{
    private val sAUTH = SpotifyAUTH()
    private val sAPI = SpotifyAPI()

    //Account Data
    private val accountNameProperty = SimpleStringProperty()
    private val accountTypeProperty = SimpleStringProperty()
    private val accountProductProperty = SimpleStringProperty()
    private val accountFollowersProperty = SimpleStringProperty()

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
        authURLProperty.value = sAUTH.getAuthorizationURL()
        squeezebox {
            fold("Spotify Account Data", expanded = true, closeable = false) {
                form {
                    fieldset {
                        addClass(fieldsetBorder)
                        button("Get Data") {
                            prefWidth = rightButtonsWidth
                            action {
                                updateAccountData()
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
                        field("Followers") {
                            textfield(accountFollowersProperty) { isEditable = false }
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

    private fun updateAccountData()
    {
        val accountData = sAPI.getAccountData()
        accountNameProperty.value = accountData.display_name
        accountTypeProperty.value = accountData.type
        accountProductProperty.value = accountData.product
        accountFollowersProperty.value = accountData.followers["total"]!!
    }

    fun showTokenData(tokenData: SpotifyAuthCallbackJson)
    {
        accessTokenProperty.value = tokenData.access_token
        generatedAtUnixTimestampProperty.value = tokenData.generatedAtUnixTimestamp.toString()
        expiresInProperty.value = tokenData.expires_in
        expireUnixTimestampProperty.value = tokenData.expireUnixTimestamp.toString()
        refreshTokenProperty.value = tokenData.refresh_token
    }
}