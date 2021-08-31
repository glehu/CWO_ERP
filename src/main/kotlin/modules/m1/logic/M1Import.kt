package modules.m1.logic

import api.logic.SpotifyAPI
import api.misc.json.*
import db.CwODB
import db.IndexContent
import interfaces.IModule
import io.ktor.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.Song
import modules.m2.Contact
import modules.m2.logic.M2DBManager
import modules.mx.activeUser
import modules.mx.logic.MXLog
import modules.mx.logic.getDefaultDate
import modules.mx.m1GlobalIndex
import modules.mx.m2GlobalIndex
import tornadofx.Controller
import java.io.RandomAccessFile
import java.time.LocalDate
import kotlin.system.measureTimeMillis

@ExperimentalSerializationApi
@InternalAPI
class M1Import : IModule, Controller() {
    override fun moduleNameLong() = "M1Import"
    override fun module() = "M1"

    val db: CwODB by inject()

    @ExperimentalSerializationApi
    fun importSpotifyAlbumList(
        albumListJson: SpotifyAlbumListJson,
        entriesAdded: Int = 0,
        updateProgress: (Pair<Int, String>) -> Unit
    ) {
        MXLog.log(module(), MXLog.LogType.INFO, "Spotify album list import start", moduleNameLong())

        var albumEntry: Song
        val raf = db.openRandomFileAccess(module(), CwODB.RafMode.READWRITE)
        val m2raf = db.openRandomFileAccess("M2", CwODB.RafMode.READWRITE)
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
        runBlocking { launch { m1GlobalIndex.writeIndexData() } }
        runBlocking { launch { m2GlobalIndex.writeIndexData() } }
        db.closeRandomFileAccess(raf)
        db.closeRandomFileAccess(m2raf)
        MXLog.log(
            module(),
            MXLog.LogType.INFO,
            "Spotify album list import end (${timeInMillis / 1000} sec)",
            moduleNameLong()
        )
    }

    @ExperimentalSerializationApi
    private fun createOrSaveTracksOfAlbum(
        trackList: SpotifyTracklistJson,
        album: Song,
        raf: RandomAccessFile,
        m2raf: RandomAccessFile,
        entriesAdded: Int = 0,
        updateProgress: (Pair<Int, String>) -> Unit
    ) {
        val m1DBManager = M1DBManager()
        var song: Song
        var uID: Int
        var pos: Long
        var byteSize: Int
        var counter = entriesAdded

        for (track: SpotifyTrackJson in trackList.tracks) {
            counter++
            //New album or existing album
            val filteredMap = m1GlobalIndex.indexList[5]!!.indexMap.filterValues {
                it.content.contains(track.id)
            }
            if (filteredMap.isEmpty()) {
                pos = -1
                byteSize = -1
                song = Song(-1, "")
            } else {
                val indexContent = filteredMap.values.first()
                uID = indexContent.uID
                pos = indexContent.pos
                byteSize = indexContent.byteSize
                song = m1DBManager.getEntry(uID, db, m1GlobalIndex.indexList[5]!!) as Song
            }

            //Spotify META Data
            song.spotifyID = track.id
            song.type = track.type
            //Generic data
            song.name = track.name
            song.inAlbum = true
            song.nameAlbum = album.name
            song.typeAlbum = album.type
            song.releaseDate = album.releaseDate

            //Do the artists exist?
            createOrSaveArtistsOfAlbum(track.artists, song, m2raf)

            //Save the song
            m1DBManager.saveEntry(
                entry = song,
                cwodb = db,
                posDB = pos,
                byteSize = byteSize,
                raf = raf,
                indexManager = m1GlobalIndex,
                indexWriteToDisk = false,
                userName = activeUser.username
            )
            MXLog.log(module(), MXLog.LogType.INFO, "Data Insertion uID ${song.uID}", moduleNameLong())
            updateProgress(Pair(counter, "Importing spotify tracks..."))
        }
    }

    @ExperimentalSerializationApi
    private fun createOrSaveAlbum(album: SpotifyAlbumJson, raf: RandomAccessFile, m2raf: RandomAccessFile): Song {
        val m1DBManager = M1DBManager()
        val song: Song
        val uID: Int
        val pos: Long
        val byteSize: Int
        var releaseDate: String = getDefaultDate()

        //New album or existing album
        val filteredMap = m1GlobalIndex.indexList[5]!!.indexMap.filterValues {
            it.content.contains(album.id)
        }
        if (filteredMap.isEmpty()) {
            pos = -1
            byteSize = -1
            song = Song(-1, "")
        } else {
            val indexContent = filteredMap.values.first()
            uID = indexContent.uID
            pos = indexContent.pos
            byteSize = indexContent.byteSize
            song = m1DBManager.getEntry(uID, db, m1GlobalIndex.indexList[5]!!) as Song
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
        m1DBManager.saveEntry(
            entry = song,
            cwodb = db,
            posDB = pos,
            byteSize = byteSize,
            raf = raf,
            indexManager = m1GlobalIndex,
            indexWriteToDisk = false,
            userName = activeUser.username
        )
        MXLog.log(module(), MXLog.LogType.INFO, "Data Insertion uID ${song.uID}", moduleNameLong())
        return song
    }

    @ExperimentalSerializationApi
    private fun createOrSaveArtistsOfAlbum(artists: List<SpotifyArtistJson>, song: Song, m2raf: RandomAccessFile) {
        val m2DBManager = M2DBManager()
        var contact: Contact
        var artistUID: Int
        var filteredMap: Map<Int, IndexContent>
        for ((artistCounter, artist: SpotifyArtistJson) in artists.withIndex()) {
            filteredMap = m2GlobalIndex.indexList[3]!!.indexMap.filterValues {
                it.content.contains(artist.id)
            }
            if (filteredMap.isEmpty()) {
                contact = Contact(-1, artist.name)
                contact.spotifyID = artist.id
                contact.birthdate = getDefaultDate()
                artistUID = m2DBManager
                    .saveEntry(
                        entry = contact,
                        cwodb = db,
                        posDB = -1,
                        byteSize = -1,
                        raf = m2raf,
                        indexManager = m2GlobalIndex,
                        indexWriteToDisk = false,
                        userName = activeUser.username
                    )
            } else {
                val indexContent = filteredMap.values.first()
                contact = m2DBManager.getEntry(indexContent.uID, db, m2GlobalIndex.indexList[3]!!) as Contact
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
