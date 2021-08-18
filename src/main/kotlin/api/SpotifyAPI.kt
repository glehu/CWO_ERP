package api

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import modules.interfaces.IAPI
import java.io.File
import java.net.URLEncoder

class SpotifyAPI : IAPI
{
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

    fun getAccessTokenFromAuthCode(authCode: String): String
    {
        lateinit var response: HttpResponse
        val client = HttpClient()
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
            }
        }
        return response.toString()
    }
}