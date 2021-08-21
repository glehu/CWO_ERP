package modules.api.logic

import interfaces.IAPI
import io.ktor.client.request.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import modules.api.json.SpotifyAlbumListJson
import modules.api.json.SpotifyUserProfileJson
import modules.mx.logic.MXAPI

class SpotifyAPI : IAPI
{
    val controller = SpotifyController()
    override val auth = SpotifyAUTH()
    override val apiName = "spotify"

    fun getAccountData(): SpotifyUserProfileJson
    {
        lateinit var userData: SpotifyUserProfileJson
        val client = auth.getAuthClient(MXAPI.Companion.AuthType.TOKEN)
        runBlocking {
            launch {
                userData = client.get("https://api.spotify.com/v1/me")
                controller.saveUserData(userData)
                client.close()
            }
        }
        return userData
    }

    fun getArtistAlbumList(artistSpotifyID: String): SpotifyAlbumListJson
    {
        var albumList = SpotifyAlbumListJson()
        if (artistSpotifyID.isNotEmpty())
        {
            val client = auth.getAuthClient(MXAPI.Companion.AuthType.TOKEN)
            runBlocking {
                launch {
                    albumList = client.get("https://api.spotify.com/v1/artists/$artistSpotifyID/albums")
                    client.close()
                }
            }
        }
        return albumList
    }
}