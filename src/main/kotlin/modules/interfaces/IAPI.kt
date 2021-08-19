package modules.interfaces

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import modules.mx.logic.MXAPI

interface IAPI
{
    val apiName: String
    val spotifyAuthEndpoint: String
    val redirectURI: String
    val redirectURIEncoded: String
    val clientID: String
    val clientSecret: String
    val responseType: String
    val scopes: String

    fun getHttpClient(authType: MXAPI.Companion.AuthType): HttpClient
    {
        return HttpClient(CIO) {
            install(JsonFeature) {
                serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            if (authType != MXAPI.Companion.AuthType.NONE)
            {
                install(Auth) {
                    if (authType == MXAPI.Companion.AuthType.TOKEN)
                    {
                        val tokenData = getAccessAndRefreshTokenFromDisk()
                        bearer {
                            loadTokens {
                                BearerTokens(tokenData.access_token, tokenData.refresh_token)
                            }
                        }
                    } else if (authType == MXAPI.Companion.AuthType.BASIC)
                    {
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

    fun getAccessAndRefreshTokenFromDisk(): ITokenData
}