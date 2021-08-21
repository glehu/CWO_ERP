package modules.m1.logic

import db.CwODB
import db.IndexContent
import interfaces.IModule
import kotlinx.serialization.ExperimentalSerializationApi
import modules.api.json.SpotifyAlbumJson
import modules.api.json.SpotifyAlbumListJson
import modules.m1.Song
import modules.m2.Contact
import modules.m2.logic.M2DBManager
import modules.mx.logic.MXLog
import modules.mx.logic.getDefaultDate
import modules.mx.m1GlobalIndex
import modules.mx.m2GlobalIndex
import tornadofx.Controller
import java.time.LocalDate
import kotlin.system.measureTimeMillis

class M1Import : IModule, Controller()
{
    override fun moduleNameLong() = "M1Import"
    override fun module() = "M1"

    val db: CwODB by inject()

    @ExperimentalSerializationApi
    fun importSpotifyAlbumList(
        albumListJson: SpotifyAlbumListJson,
        updateProgress: (Pair<Int, String>) -> Unit
    )
    {
        MXLog.log(module(), MXLog.LogType.INFO, "Spotify album list import start", moduleNameLong())
        val raf = db.openRandomFileAccess(module(), CwODB.RafMode.READWRITE)
        val m2raf = db.openRandomFileAccess("M2", CwODB.RafMode.READWRITE)
        val m1DBManager = M1DBManager()
        val m2DBManager = M2DBManager()
        var filteredMap: Map<Int, IndexContent>
        var song: Song
        var contact: Contact
        var counter = 0
        var uID: Int
        var pos: Long
        var byteSize: Int
        val timeInMillis = measureTimeMillis {
            for (album: SpotifyAlbumJson in albumListJson.albums)
            {
                counter++
                updateProgress(Pair(counter, "Importing spotify albums..."))

                //New album or existing album
                filteredMap = m1GlobalIndex.indexList[5]!!.indexMap.filterValues {
                    it.content.contains(album.id)
                }
                if (filteredMap.isEmpty())
                {
                    pos = -1
                    byteSize = -1
                    song = Song(-1, "")
                } else
                {
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
                song.vocalist = album.artists[0].name
                val releaseDate = LocalDate.parse(album.releaseDate)
                song.releaseDate =
                    releaseDate.dayOfMonth
                        .toString().padStart(2, '0') +
                            ".${
                                releaseDate.monthValue
                                    .toString().padStart(2, '0')
                            }" +
                            ".${releaseDate.year}"

                //Does the artist exist?
                filteredMap = m2GlobalIndex.indexList[3]!!.indexMap.filterValues {
                    it.content.contains(album.artists[0].id)
                }
                if (filteredMap.isEmpty())
                {
                    contact = Contact(-1, song.vocalist)
                    contact.spotifyID = album.artists[0].id
                    contact.birthdate = getDefaultDate()
                    song.vocalistUID = m2DBManager.saveEntry(
                        entry = contact,
                        cwodb = db,
                        posDB = -1,
                        byteSize = -1,
                        raf = m2raf,
                        indexManager = m2GlobalIndex,
                        indexWriteToDisk = true
                    )
                } else
                {
                    val indexContent = filteredMap.values.first()
                    contact = m2DBManager.getEntry(indexContent.uID, db, m2GlobalIndex.indexList[3]!!) as Contact
                    song.vocalistUID = contact.uID
                }

                //Save the song
                m1DBManager.saveEntry(
                    entry = song,
                    cwodb = db,
                    posDB = pos,
                    byteSize = byteSize,
                    raf = raf,
                    indexManager = m1GlobalIndex,
                    indexWriteToDisk = true
                )
                MXLog.log(module(), MXLog.LogType.INFO, "Data Insertion uID ${song.uID}", moduleNameLong())
            }
        }
        db.closeRandomFileAccess(raf)
        MXLog.log(
            module(),
            MXLog.LogType.INFO,
            "Spotify album list import end (${timeInMillis / 1000} sec)",
            moduleNameLong()
        )
    }
}
