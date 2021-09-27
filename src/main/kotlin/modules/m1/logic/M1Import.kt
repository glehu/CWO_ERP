package modules.m1.logic

import api.logic.SpotifyAPI
import api.misc.json.*
import db.CwODB
import db.IndexContent
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.M1Song
import modules.m2.M2Contact
import modules.mx.logic.MXLog
import modules.mx.logic.getDefaultDate
import modules.mx.logic.indexFormat
import modules.mx.m1GlobalIndex
import modules.mx.m2GlobalIndex
import tornadofx.Controller
import java.io.RandomAccessFile
import java.time.LocalDate
import kotlin.system.measureTimeMillis

@ExperimentalSerializationApi
@InternalAPI
class M1Import : IModule, Controller() {
    override val moduleNameLong = "M1Import"
    override val module = "M1"
    override fun getIndexManager(): IIndexManager {
        return m1GlobalIndex
    }

    @ExperimentalSerializationApi
    suspend fun importSpotifyAlbumList(
        albumListJson: SpotifyAlbumListJson,
        entriesAdded: Int = 0,
        updateProgress: (Pair<Int, String>) -> Unit
    ) {
        log(MXLog.LogType.INFO, "Spotify album list import start")

        var albumEntry: M1Song
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
            launch { m1GlobalIndex.writeIndexData() }
            launch { m2GlobalIndex.writeIndexData() }
        }
        CwODB.closeRandomFileAccess(raf)
        CwODB.closeRandomFileAccess(m2raf)
        log(MXLog.LogType.INFO, "Spotify album list import end (${timeInMillis / 1000} sec)")
    }

    @ExperimentalSerializationApi
    private suspend fun createOrSaveTracksOfAlbum(
        trackList: SpotifyTracklistJson,
        album: M1Song,
        raf: RandomAccessFile,
        m2raf: RandomAccessFile,
        entriesAdded: Int = 0,
        updateProgress: (Pair<Int, String>) -> Unit
    ) {
        var song: M1Song
        var uID: Int
        var counter = entriesAdded

        for (track: SpotifyTrackJson in trackList.tracks) {
            counter++
            //New album or existing album
            val filteredMap = m1GlobalIndex.indexList[5]!!.indexMap.filterValues {
                it.content.contains(track.id)
            }
            if (filteredMap.isEmpty()) {
                song = M1Song(-1, "")
            } else {
                val indexContent = filteredMap.values.first()
                uID = indexContent.uID
                song = get(uID) as M1Song
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
    ): M1Song {
        val song: M1Song
        var releaseDate: String = getDefaultDate()

        //New album or existing album
        val filteredMap = m1GlobalIndex.indexList[5]!!.indexMap.filterValues {
            it.content.contains(indexFormat(album.id).uppercase())
        }
        song = if (filteredMap.isEmpty()) {
            M1Song(-1, "")
        } else {
            get(filteredMap.keys.first()) as M1Song
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
        song: M1Song,
        m2raf: RandomAccessFile
    ) {
        var contact: M2Contact
        var artistUID: Int
        var filteredMap: Map<Int, IndexContent>
        for ((artistCounter, artist: SpotifyArtistJson) in artists.withIndex()) {
            filteredMap = m2GlobalIndex.indexList[3]!!.indexMap.filterValues {
                it.content.contains(indexFormat(artist.id).uppercase())
            }
            if (filteredMap.isEmpty()) {
                contact = M2Contact(-1, artist.name)
                contact.spotifyID = artist.id
                contact.birthdate = getDefaultDate()
                artistUID = m2GlobalIndex.save(
                    entry = contact,
                    raf = m2raf,
                    indexWriteToDisk = false,
                )
            } else {
                contact = m2GlobalIndex.get(filteredMap.keys.first()) as M2Contact
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
