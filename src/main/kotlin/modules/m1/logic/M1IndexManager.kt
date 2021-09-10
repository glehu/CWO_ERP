package modules.m1.logic

import db.Index
import db.IndexContent
import interfaces.IIndexManager
import interfaces.IModule
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m1.Song
import modules.mx.logic.MXLog
import modules.mx.logic.indexFormat
import tornadofx.Controller
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalSerializationApi
class M1IndexManager : IModule, IIndexManager, Controller() {
    override val moduleNameLong = "M1IndexManager"
    override val module = "M1"
    override var lastChangeDateHex: String = ""
    override var lastChangeDateUTC: String = ""
    override var lastChangeDateLocal: String = ""
    override var lastChangeUser: String = ""
    //*************************************************
    //********************** Global Data **************
    //*************************************************

    override val indexList = mutableMapOf<Int, Index>()
    override var lastUID = AtomicInteger(-1)

    init {
        MXLog.log(module, MXLog.LogType.INFO, "Initializing index manager...", moduleNameLong)
        indexList[0] = getIndex(0)
        indexList[1] = getIndex(1)
        indexList[2] = getIndex(2)
        indexList[3] = getIndex(3)
        indexList[4] = getIndex(4)
        indexList[5] = getIndex(5)
        lastUID = updateLastUID()
        getLastChangeDates()
        MXLog.log(module, MXLog.LogType.INFO, "Index manager ready", moduleNameLong)
    }

    override fun getIndexUserSelection(): ArrayList<String> {
        return arrayListOf("1-Name", "2-Vocalist", "3-Producer", "4-Genre")
    }

    override fun indexEntry(
        entry: Any,
        posDB: Long,
        byteSize: Int,
        writeToDisk: Boolean,
        userName: String
    ) = runBlocking {
        entry as Song
        buildIndex0(entry, posDB, byteSize)
        buildIndex1(entry, posDB, byteSize)
        buildIndex2(entry, posDB, byteSize)
        buildIndex3(entry, posDB, byteSize)
        buildIndex4(entry, posDB, byteSize)
        buildIndex5(entry, posDB, byteSize)
        if (writeToDisk) launch {
            writeIndexData()
        }
        setLastChangeData(entry.uID, userName)
    }

    override suspend fun writeIndexData() {
        getIndexFile(0).writeText(Json.encodeToString(indexList[0]))
        getIndexFile(1).writeText(Json.encodeToString(indexList[1]))
        getIndexFile(2).writeText(Json.encodeToString(indexList[2]))
        getIndexFile(3).writeText(Json.encodeToString(indexList[3]))
        getIndexFile(4).writeText(Json.encodeToString(indexList[4]))
        getIndexFile(5).writeText(Json.encodeToString(indexList[5]))
    }

    //**** **** **** **** **** INDICES **** **** **** **** ****
    //Index 0 (Song.uID)
    override fun buildIndex0(entry: Any, posDB: Long, byteSize: Int) {
        entry as Song
        indexList[0]!!.indexMap[entry.uID] = IndexContent(entry.uID, "${entry.uID}", posDB, byteSize)
    }

    //Index 1 (Song.name)
    private fun buildIndex1(song: Song, posDB: Long, byteSize: Int) {
        val formatted = indexFormat(song.name).uppercase()
        indexList[1]!!.indexMap[song.uID] = IndexContent(song.uID, formatted, posDB, byteSize)
    }

    //Index 2 (Song.vocalist)
    private fun buildIndex2(song: Song, posDB: Long, byteSize: Int) {
        val formatted = indexFormat(song.vocalist)
        indexList[2]!!.indexMap[song.uID] = IndexContent(song.uID, formatted, posDB, byteSize)
    }

    //Index 3 (Song.producer)
    private fun buildIndex3(song: Song, posDB: Long, byteSize: Int) {
        val formatted = indexFormat(song.producer)
        indexList[3]!!.indexMap[song.uID] = IndexContent(song.uID, formatted, posDB, byteSize)
    }

    //Index 4 (Song.genre)
    private fun buildIndex4(song: Song, posDB: Long, byteSize: Int) {
        val formatted = indexFormat(song.genre)
        indexList[4]!!.indexMap[song.uID] = IndexContent(song.uID, formatted, posDB, byteSize)
    }

    //Index 5 (Song.spotifyID)
    private fun buildIndex5(song: Song, posDB: Long, byteSize: Int) {
        val formatted = song.spotifyID
        indexList[5]!!.indexMap[song.uID] = IndexContent(song.uID, formatted, posDB, byteSize)
    }
}