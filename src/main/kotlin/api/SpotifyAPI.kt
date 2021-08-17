package api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import modules.interfaces.IAPI
import java.io.File
import java.net.URLEncoder

class SpotifyAPI : IAPI
{
    suspend fun authorize()
    {
        val id = "172f78362a5447c18cc93a75cdb16dfe"
        val secret = File("confCred/spotifyApp").readText()
        val httpClient = HttpClient()
        val httpResponse = httpClient.request<HttpResponse> {
            url("https://accounts.spotify.com/authorize")
            method = HttpMethod.Get
            headers {
                append("client_id", id)
                append("response_type", "code")
                append(
                    "redirect_uri",
                    URLEncoder.encode(
                        "https://github.com/glehu/CWO_ERP#readme",
                        "utf-8"
                    )
                )
                append("state", "ded728fb04a7")
                append(
                    "scope",
                    URLEncoder.encode(
                        "user-read-private user-library-read playlist-read-collaborative",
                        "utf-8"
                    )
                )
            }
        }
        val stringResponse = httpResponse.receive<String>()
        print(stringResponse)
        httpClient.close()
    }
}