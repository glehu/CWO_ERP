package api.logic

import api.misc.json.*
import interfaces.IAPI
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.client.request.*
import io.ktor.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.discographyIndexManager
import modules.mx.logic.APIFileManager
import modules.mx.logic.Log

@InternalAPI
@ExperimentalSerializationApi
class SpotifyAPI : IModule, IAPI {
  override val moduleNameLong = "SpotifyAPI"
  override val module = "M1"
  override fun getIndexManager(): IIndexManager {
    return discographyIndexManager!!
  }

  val controller = SpotifyController()
  override val auth = SpotifyAUTH()
  override val apiName = "spotify"

  fun getAccountData(): SpotifyUserProfileJson {
    lateinit var userData: SpotifyUserProfileJson
    val client = auth.getAuthClient(APIFileManager.Companion.AuthType.TOKEN)
    runBlocking {
      launch {
        userData = client.get("https://api.spotify.com/v1/me")
        log(Log.LogType.COM, "Spotify account data received")
        controller.saveUserData(userData)
        client.close()
      }
    }
    return userData
  }

  fun getArtistAlbumList(artistSpotifyID: String): ArrayList<SpotifyAlbumListJson> {
    var albumList: SpotifyAlbumListJson
    val albumListTotal = ArrayList<SpotifyAlbumListJson>()
    var finished = false
    var url = "https://api.spotify.com/v1/artists/$artistSpotifyID/albums?limit=50"
    if (artistSpotifyID.isNotEmpty()) {
      val client = auth.getAuthClient(APIFileManager.Companion.AuthType.TOKEN)
      runBlocking {
        launch {
          while (!finished) {
            albumList = client.get(url)
            albumListTotal.add(albumList)
            log(Log.LogType.COM, "Spotify album list received")
            if (albumList.next == null) {
              finished = true
            } else {
              url = albumList.next!!
            }
          }
          client.close()
        }
      }
    }
    return albumListTotal
  }

  fun getArtist(artistSpotifyID: String): SpotifyArtistJson {
    lateinit var artistData: SpotifyArtistJson
    val client = auth.getAuthClient(APIFileManager.Companion.AuthType.TOKEN)
    runBlocking {
      launch {
        artistData = client.get("https://api.spotify.com/v1/artists/$artistSpotifyID")
        log(Log.LogType.COM, "Spotify artist data received")
        client.close()
      }
    }
    return artistData
  }

  fun getMultipleArtists(spotifyIDs: List<String>): SpotifyArtistListJson {
    var artistDataList = SpotifyArtistListJson()
    val separatedIDs: StringBuilder = StringBuilder()
    for ((counter, id) in spotifyIDs.withIndex()) {
      separatedIDs.append(id)
      if (counter != spotifyIDs.size) separatedIDs.append(",")
    }
    val client = auth.getAuthClient(APIFileManager.Companion.AuthType.TOKEN)
    runBlocking {
      launch {
        artistDataList = client.get("https://api.spotify.com/v1/artists/${separatedIDs}")
        log(Log.LogType.COM, "Spotify artist data received")
        client.close()
      }
    }
    return artistDataList
  }

  fun getSongListFromAlbum(albumSpotifyID: String): ArrayList<SpotifyTracklistJson> {
    var songList: SpotifyTracklistJson
    val songListTotal = ArrayList<SpotifyTracklistJson>()
    var finished = false
    var url = "https://api.spotify.com/v1/albums/$albumSpotifyID/tracks?limit=50"
    if (albumSpotifyID.isNotEmpty()) {
      val client = auth.getAuthClient(APIFileManager.Companion.AuthType.TOKEN)
      runBlocking {
        launch {
          while (!finished) {
            songList = client.get(url)
            songListTotal.add(songList)
            log(Log.LogType.COM, "Spotify song list received")
            if (songList.next == null) {
              finished = true
            } else {
              url = songList.next!!
            }
          }
          client.close()
        }
      }
    }
    return songListTotal
  }
}
