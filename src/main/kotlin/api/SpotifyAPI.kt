package api

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.interfaces.IAPI
import modules.mx.logic.MXAPI
import modules.mx.logic.MXTimestamp
import server.SpotifyAuthCallbackJson
import java.io.File
import java.net.URLEncoder

class SpotifyAPI : IAPI
{
    override val apiName = "spotify"

    private val spotifyAuthEndpoint = "https://accounts.spotify.com/authorize"
    private val redirectURI = "http://localhost:8000/authcallback/spotify"
    private val redirectURIEncoded = URLEncoder.encode(redirectURI, "utf-8")
    private val clientID = "172f78362a5447c18cc93a75cdb16dfe"
    private val clientSecret = File("confCred/spotifyApp").readText()
    private val responseType = "code"
    private val scopes = "user-read-playback-position%20" +
            "playlist-read-private%20" +
            "user-read-email%20" +
            "user-read-private%20" +
            "user-library-read%20" +
            "playlist-read-collaborative"

    fun getAuthorizationURL(): String
    {
        return spotifyAuthEndpoint +
                "?client_id=$clientID" +
                "&response_type=$responseType" +
                "&redirect_uri=$redirectURIEncoded" +
                "&scope=$scopes" +
                "&state=b78b07157ced"
    }

    fun getAccessTokenFromAuthCode(authCode: String): SpotifyAuthCallbackJson
    {
        lateinit var response: SpotifyAuthCallbackJson
        val client = HttpClient {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
        }
        runBlocking {
            launch {
                response = client.post("https://accounts.spotify.com/api/token") {
                    body = FormDataContent(Parameters.build {
                        append("grant_type", "authorization_code")
                        append("code", authCode)
                        append("redirect_uri", redirectURI)
                        append("client_id", clientID)
                        append("client_secret", clientSecret)
                    })
                }
                client.close()
                saveAccessAndRefreshToken(response)
            }
        }
        return response
    }

    private fun saveAccessAndRefreshToken(response: SpotifyAuthCallbackJson)
    {
        val tokenFile = MXAPI().getAPITokenFile(apiName)
        response.initialize()
        tokenFile.writeText(Json.encodeToString(response))
    }


    fun getAccessAndRefreshTokenFromDisk(): SpotifyAuthCallbackJson
    {
        val tokenData: SpotifyAuthCallbackJson
        val tokenFile = MXAPI().getAPITokenFile(apiName)
        val fileContent = tokenFile.readText()
        tokenData = if (fileContent.isNotEmpty())
        {
            Json.decodeFromString(tokenFile.readText())
        } else
        {
            SpotifyAuthCallbackJson("?", "?", "?", 0, "?")
        }
        return tokenData
    }
}