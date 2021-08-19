package modules.api.logic

import interfaces.IAPIAUTH
import interfaces.ITokenData
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.api.gui.MGXSpotify
import modules.mx.getClientSecretFile
import modules.mx.logic.MXAPI
import modules.mx.logic.MXAPI.Companion.getAPITokenFile
import modules.mx.logic.MXTimestamp
import modules.api.json.SpotifyAuthCallbackJson
import tornadofx.find
import java.net.URLEncoder

class SpotifyAUTH : IAPIAUTH
{
    override val apiName = "spotify"

    override val spotifyAuthEndpoint = "https://accounts.spotify.com/authorize"
    override val redirectURI = "http://localhost:8000/authcallback/spotify"
    override val redirectURIEncoded = URLEncoder.encode(redirectURI, "utf-8")!!
    override val clientID = "172f78362a5447c18cc93a75cdb16dfe"
    override val clientSecret = getClientSecretFile(apiName).readText()
    override val responseType = "code"
    override val scopes = "user-read-playback-position%20" +
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
                "&state=b78b07157ced" +
                "&show_dialog=true"
    }

    fun getAccessTokenFromAuthCode(authCode: String): SpotifyAuthCallbackJson
    {
        lateinit var response: SpotifyAuthCallbackJson
        val client = getAuthClient(MXAPI.Companion.AuthType.NONE)
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

    private fun saveAccessAndRefreshToken(response: ITokenData)
    {
        response as SpotifyAuthCallbackJson
        val tokenFile = getAPITokenFile(apiName)
        response.initialize()
        val sJson = Json.encodeToString(response)
        tokenFile.writeText(sJson)
    }

    override fun getAccessAndRefreshTokenFromDisk(checkExpired: Boolean): ITokenData
    {
        var tokenData: SpotifyAuthCallbackJson
        val tokenFile = getAPITokenFile(apiName)
        val fileContent = tokenFile.readText()
        if (fileContent.isNotEmpty())
        {
            tokenData = Json.decodeFromString(tokenFile.readText())
            if (checkExpired)
            {
                if (tokenData.expireUnixTimestamp <= MXTimestamp.getUnixTimestamp())
                {
                    refreshAccessToken()
                    tokenData = Json.decodeFromString(tokenFile.readText())
                    find<MGXSpotify>().showTokenData(getAccessAndRefreshTokenFromDisk() as SpotifyAuthCallbackJson)
                }
            }
        } else tokenData = SpotifyAuthCallbackJson("?", "?", "?", 0, "?")
        return tokenData
    }

    fun refreshAccessToken()
    {
        var tokenDataNew: SpotifyAuthCallbackJson
        val tokenData = getAccessAndRefreshTokenFromDisk() as SpotifyAuthCallbackJson
        val client = getAuthClient(MXAPI.Companion.AuthType.NONE)
        runBlocking {
            launch {
                tokenDataNew = client.post("https://accounts.spotify.com/api/token") {
                    body = FormDataContent(Parameters.build {
                        append("grant_type", "refresh_token")
                        append("refresh_token", tokenData.refresh_token)
                        append("client_id", clientID)
                        append("client_secret", clientSecret)
                    })
                }
                client.close()
                tokenData.access_token = tokenDataNew.access_token
                saveAccessAndRefreshToken(tokenData)
            }
        }
    }
}