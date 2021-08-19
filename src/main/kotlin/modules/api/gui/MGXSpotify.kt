package modules.api.gui

import modules.api.logic.SpotifyAPI
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import modules.mx.rightButtonsWidth
import server.SpotifyAuthCallbackJson
import tornadofx.*

class MGXSpotify : View("Spotify API")
{
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
        showTokenData(SpotifyAPI().getAccessAndRefreshTokenFromDisk() as SpotifyAuthCallbackJson)
        squeezebox {
            fold("Spotify Account Data", expanded = true, closeable = false) {
                form {
                    fieldset {
                        button("Update") {
                            prefWidth = rightButtonsWidth
                            action {
                                updateAccountData()
                            }
                            isDisable = (accessTokenProperty.value.isEmpty())
                        }
                        hbox {
                            field("Account Name") {
                                textfield(accountNameProperty) {
                                    isEditable = false
                                }
                            }
                            field("Account Type") {
                                textfield(accountTypeProperty) {
                                    isEditable = false
                                }
                            }
                            field("Account Product") {
                                textfield(accountProductProperty) {
                                    isEditable = false
                                }
                            }
                        }
                        field("Followers") {
                            textfield(accountFollowersProperty) {
                                isEditable = false
                            }
                        }
                    }
                }
            }
            fold("Spotify Token Data", expanded = false, closeable = false) {
                form {
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
                        }
                        hbox {
                            field("Access Token") {
                                textfield(accessTokenProperty) {
                                    prefWidth = 1500.0
                                    isEditable = false
                                }
                            }
                            button("Refresh") {
                                prefWidth = rightButtonsWidth
                                action {
                                    SpotifyAPI().refreshAccessToken()
                                    showTokenData(
                                        SpotifyAPI().getAccessAndRefreshTokenFromDisk() as SpotifyAuthCallbackJson
                                    )
                                }
                            }
                        }
                        hbox {
                            field("Generated at (UNIXTIME)") {
                                textfield(generatedAtUnixTimestampProperty) {
                                    isEditable = false
                                }
                            }
                            field("Expires in (seconds)") {
                                textfield(expiresInProperty) {
                                    isEditable = false
                                }
                            }
                            field("at (UNIXTIME)") {
                                textfield(expireUnixTimestampProperty) {
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
            }
        }
    }

    private fun updateAccountData()
    {
        val accountData = SpotifyAPI().getAccountData()
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