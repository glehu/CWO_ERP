package modules.api.logic

import interfaces.IAPI
import interfaces.IModule
import io.ktor.client.request.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.api.json.SpotifyAlbumListJson
import modules.api.json.SpotifyUserProfileJson
import modules.mx.logic.MXAPI
import modules.mx.logic.MXLog

@ExperimentalSerializationApi
class SpotifyAPI : IModule, IAPI
{
    override fun moduleNameLong() = "SpotifyAPI"
    override fun module() = "M1"

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
                MXLog.log(module(), MXLog.LogType.COM, "Spotify account data received", moduleNameLong())
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
                    albumList = client.get(
                        "https://api.spotify.com/v1/artists/$artistSpotifyID/albums?limit=50"
                    )
                    MXLog.log(module(), MXLog.LogType.COM, "Spotify album list received", moduleNameLong())
                    client.close()
                }
            }
        }
        return albumList
    }
}