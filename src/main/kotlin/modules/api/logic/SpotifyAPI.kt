package modules.api.logic

import interfaces.IAPI
import io.ktor.client.request.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import modules.api.json.SpotifyUserProfileJson
import modules.mx.logic.MXAPI

class SpotifyAPI : IAPI
{
    override val auth = SpotifyAUTH()

    fun getAccountData(): SpotifyUserProfileJson
    {
        lateinit var response: SpotifyUserProfileJson
        val client = auth.getAuthClient(MXAPI.Companion.AuthType.TOKEN)
        runBlocking {
            launch {
                response = client.get("https://api.spotify.com/v1/me")
                client.close()
            }
        }
        return response
    }
}