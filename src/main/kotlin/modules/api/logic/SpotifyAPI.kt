package modules.api.logic

import io.ktor.client.request.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import modules.mx.logic.MXAPI
import modules.api.json.SpotifyUserProfileJson

class SpotifyAPI
{
    private val spotifyAUTH = SpotifyAUTH()
    fun getAccountData(): SpotifyUserProfileJson
    {
        lateinit var response: SpotifyUserProfileJson
        val client = spotifyAUTH.getAuthClient(MXAPI.Companion.AuthType.TOKEN)
        runBlocking {
            launch {
                response = client.get("https://api.spotify.com/v1/me")
                client.close()
            }
        }
        return response
    }
}