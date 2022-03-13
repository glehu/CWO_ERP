package modules.m1.logic

import api.logic.SpotifyAPI
import api.misc.json.SpotifyAlbumJson
import api.misc.json.SpotifyAlbumListJson
import api.misc.json.SpotifyArtistJson
import api.misc.json.SpotifyTrackJson
import api.misc.json.SpotifyTracklistJson
import db.CwODB
import db.IndexContent
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.Song
import modules.m2.Contact
import modules.mx.contactIndexManager
import modules.mx.discographyIndexManager
import modules.mx.logic.Log
import modules.mx.logic.getDefaultDate
import modules.mx.logic.indexFormat
import tornadofx.Controller
import java.io.RandomAccessFile
import java.time.LocalDate
import kotlin.system.measureTimeMillis

@ExperimentalSerializationApi
@InternalAPI
class DiscographyImport : IModule, Controller() {
  override val moduleNameLong = "DiscographyImport"
  override val module = "M1"
  override fun getIndexManager(): IIndexManager {
    return discographyIndexManager!!
  }

  @ExperimentalSerializationApi
  suspend fun importSpotifyAlbumList(
    albumListJson: SpotifyAlbumListJson,
    entriesAdded: Int = 0,
    updateProgress: (Pair<Int, String>) -> Unit
  ) {
    log(Log.LogType.INFO, "Spotify album list import start")

    var albumEntry: Song
    val raf = CwODB.openRandomFileAccess(module, CwODB.CwODB.RafMode.READWRITE)
    val m2raf = CwODB.openRandomFileAccess("M2", CwODB.CwODB.RafMode.READWRITE)
    var counter = entriesAdded
    val timeInMillis = measureTimeMillis {
      for (album: SpotifyAlbumJson in albumListJson.albums) {
        counter++
        albumEntry = createOrSaveAlbum(album, raf, m2raf)
        for (trackList: SpotifyTracklistJson in SpotifyAPI().getSongListFromAlbum(album.id)) {
          createOrSaveTracksOfAlbum(trackList, albumEntry, raf, m2raf, counter)
          {
            counter = it.first
          }
        }
        updateProgress(Pair(counter, "Importing spotify albums..."))
      }
    }
    coroutineScope {
      launch { discographyIndexManager!!.writeIndexData() }
      launch { contactIndexManager!!.writeIndexData() }
    }
    CwODB.closeRandomFileAccess(raf)
    CwODB.closeRandomFileAccess(m2raf)
    log(Log.LogType.INFO, "Spotify album list import end (${timeInMillis / 1000} sec)")
  }

  @ExperimentalSerializationApi
  private suspend fun createOrSaveTracksOfAlbum(
    trackList: SpotifyTracklistJson,
    album: Song,
    raf: RandomAccessFile,
    m2raf: RandomAccessFile,
    entriesAdded: Int = 0,
    updateProgress: (Pair<Int, String>) -> Unit
  ) {
    var song: Song
    var uID: Int
    var counter = entriesAdded

    for (track: SpotifyTrackJson in trackList.tracks) {
      counter++
      //New album or existing album
      val filteredMap = discographyIndexManager!!.indexList[5]!!.indexMap.filterValues {
        it.content.contains(track.id)
      }
      if (filteredMap.isEmpty()) {
        song = Song(-1, "")
      } else {
        val indexContent = filteredMap.values.first()
        uID = indexContent.uID
        song = get(uID) as Song
      }

      //Spotify META Data
      song.spotifyID = track.id
      song.type = track.type
      //Generic data
      song.name = track.name
      song.inAlbum = true
      song.nameAlbum = album.name
      song.typeAlbum = album.type
      song.albumUID = album.uID
      song.releaseDate = album.releaseDate

      //Do the artists exist?
      createOrSaveArtistsOfAlbum(track.artists, song, m2raf)

      //Save the song
      save(
        entry = song,
        raf = raf,
        indexWriteToDisk = false,
      )
      updateProgress(Pair(counter, "Importing spotify tracks..."))
    }
  }

  @ExperimentalSerializationApi
  private suspend fun createOrSaveAlbum(
    album: SpotifyAlbumJson,
    raf: RandomAccessFile,
    m2raf: RandomAccessFile
  ): Song {
    val song: Song
    var releaseDate: String = getDefaultDate()

    //New album or existing album
    val filteredMap = discographyIndexManager!!.indexList[5]!!.indexMap.filterValues {
      it.content.contains(indexFormat(album.id).uppercase())
    }
    song = if (filteredMap.isEmpty()) {
      Song(-1, "")
    } else {
      get(filteredMap.keys.first()) as Song
    }

    //Spotify META Data
    song.spotifyID = album.id
    song.type = album.albumType
    //Generic data
    song.name = album.name

    when (album.releaseDatePrecision) {
      "day" -> releaseDate = album.releaseDate
      "month" -> releaseDate = album.releaseDate + "-01"
      "year" -> releaseDate = album.releaseDate + "-01-01"
    }
    val releaseDateParsed = LocalDate.parse(releaseDate)
    song.releaseDate =
      releaseDateParsed.dayOfMonth.toString().padStart(2, '0') +
              ".${
                releaseDateParsed.monthValue
                  .toString().padStart(2, '0')
              }" +
              ".${releaseDateParsed.year}"

    //Do the artists exist?
    createOrSaveArtistsOfAlbum(album.artists, song, m2raf)

    //Save the song
    save(
      entry = song,
      raf = raf,
      indexWriteToDisk = false,
    )
    return song
  }

  @ExperimentalSerializationApi
  private suspend fun createOrSaveArtistsOfAlbum(
    artists: List<SpotifyArtistJson>,
    song: Song,
    m2raf: RandomAccessFile
  ) {
    var contact: Contact
    var artistUID: Int
    var filteredMap: Map<Int, IndexContent>
    for ((artistCounter, artist: SpotifyArtistJson) in artists.withIndex()) {
      filteredMap = contactIndexManager!!.indexList[3]!!.indexMap.filterValues {
        it.content.contains(indexFormat(artist.id).uppercase())
      }
      if (filteredMap.isEmpty()) {
        contact = Contact(-1, artist.name)
        contact.spotifyID = artist.id
        contact.birthdate = getDefaultDate()
        artistUID = contactIndexManager!!.save(
          entry = contact,
          raf = m2raf,
          indexWriteToDisk = false,
        )
      } else {
        contact = contactIndexManager!!.get(filteredMap.keys.first()) as Contact
        artistUID = contact.uID
      }
      when (artistCounter) {
        0 -> {
          song.vocalist = artist.name
          song.vocalistUID = artistUID
        }

        1 -> {
          song.coVocalist1 = artist.name
          song.coVocalist1UID = artistUID
        }

        2 -> {
          song.coVocalist2 = artist.name
          song.coVocalist2UID = artistUID
        }
      }
    }
  }
}
