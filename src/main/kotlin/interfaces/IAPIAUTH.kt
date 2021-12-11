package interfaces

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import modules.mx.logic.API

interface IAPIAUTH : IAPI {
    override val apiName: String
    val spotifyAuthEndpoint: String
    val redirectURI: String
    val redirectURIEncoded: String
    val clientID: String
    val clientSecret: String
    val responseType: String
    val scopes: String

    fun getAuthorizationURL(): String

    fun getAccessTokenFromAuthCode(authCode: String): ITokenData

    fun saveTokenData(tokenData: ITokenData) {
        val tokenFile = API.getAPITokenFile(apiName)
        tokenData.initialize()
        val sJson = serializeTokenData(tokenData)
        tokenFile.writeText(sJson)
    }

    fun serializeTokenData(tokenData: ITokenData): String

    fun refreshAccessToken()

    fun getAuthClient(authType: API.Companion.AuthType): HttpClient {
        return HttpClient(CIO) {
            install(JsonFeature) {
                serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            if (authType != API.Companion.AuthType.NONE) {
                install(Auth) {
                    if (authType == API.Companion.AuthType.TOKEN) {
                        val tokenData = getAccessAndRefreshTokenFromDisk(checkExpired = true)
                        bearer {
                            loadTokens {
                                BearerTokens(tokenData.accessToken, tokenData.refreshToken)
                            }
                        }
                    } else if (authType == API.Companion.AuthType.BASIC) {
                        basic {
                            credentials {
                                BasicAuthCredentials(username = clientID, password = clientSecret)
                            }
                            sendWithoutRequest { request ->
                                request.url.host == "0.0.0.0"
                            }
                        }
                    }
                }
            }
        }
    }

    fun getAccessAndRefreshTokenFromDisk(checkExpired: Boolean = false): ITokenData
}
