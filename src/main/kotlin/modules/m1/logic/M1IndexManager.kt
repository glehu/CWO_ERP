package modules.m1.logic

import db.CwODB
import db.Index
import db.IndexContent
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import modules.IIndexManager
import modules.IModule
import modules.mx.logic.MXLog
import modules.mx.misc.indexFormat
import tornadofx.Controller
import modules.m1.Song as Song

@ExperimentalSerializationApi
class M1IndexManager: IModule, IIndexManager, Controller()
{
    override fun moduleName() = "M1IndexManager"
    override val indexList = mutableMapOf<Int, Index>()

    val db: CwODB by inject()

    init
    {
        MXLog.log("M1", MXLog.LogType.INFO, "Initializing index manager...", moduleName())
        indexList[0] = db.getIndex("M1", 0)
        indexList[1] = db.getIndex("M1", 1)
        indexList[2] = db.getIndex("M1", 2)
        indexList[3] = db.getIndex("M1", 3)
        indexList[4] = db.getIndex("M1", 4)
        MXLog.log("M1", MXLog.LogType.INFO, "Index manager ready", moduleName())
    }

    override fun getIndexUserSelection(): ArrayList<String>
    {
        return arrayListOf("1-Name", "2-Vocalist", "3-Producer", "4-Genre")
    }

    @ExperimentalSerializationApi
    override fun indexEntry(entry: Any, posDB: Long, byteSize: Int, writeToDisk: Boolean) = runBlocking {
        entry as Song
        buildIndex0(entry, posDB, byteSize)
        buildIndex1(entry, posDB, byteSize)
        buildIndex2(entry, posDB, byteSize)
        buildIndex3(entry, posDB, byteSize)
        buildIndex4(entry, posDB, byteSize)
        if (writeToDisk) launch { writeIndexData() }
    }

    override suspend fun writeIndexData()
    {
        db.getIndexFile("M1", 0).writeText(Json.encodeToString(indexList[0]))
        db.getIndexFile("M1", 1).writeText(Json.encodeToString(indexList[1]))
        db.getIndexFile("M1", 2).writeText(Json.encodeToString(indexList[2]))
        db.getIndexFile("M1", 3).writeText(Json.encodeToString(indexList[3]))
        db.getIndexFile("M1", 4).writeText(Json.encodeToString(indexList[4]))
    }

    //**** **** **** **** **** INDICES **** **** **** **** ****
    //Index 0 (Song.uID)
    override fun buildIndex0(entry: Any, posDB: Long, byteSize: Int)
    {
        entry as Song
        indexList[0]!!.indexMap[entry.uID] = IndexContent(entry.uID, "${entry.uID}", posDB, byteSize)
    }

    //Index 1 (Song.name)
    private fun buildIndex1(song: Song, posDB: Long, byteSize: Int)
    {
        val formatted = indexFormat(song.name).uppercase()
        indexList[1]!!.indexMap[song.uID] = IndexContent(song.uID, formatted, posDB, byteSize)
    }

    //Index 2 (Song.vocalist)
    private fun buildIndex2(song: Song, posDB: Long, byteSize: Int)
    {
        val formatted = indexFormat(song.vocalist)
        indexList[2]!!.indexMap[song.uID] = IndexContent(song.uID, formatted, posDB, byteSize)
    }

    //Index 3 (Song.producer)
    private fun buildIndex3(song: Song, posDB: Long, byteSize: Int)
    {
        val formatted = indexFormat(song.producer)
        indexList[3]!!.indexMap[song.uID] = IndexContent(song.uID, formatted, posDB, byteSize)
    }

    //Index 4 (Song.genre)
    private fun buildIndex4(song: Song, posDB: Long, byteSize: Int)
    {
        val formatted = indexFormat(song.genre)
        indexList[4]!!.indexMap[song.uID] = IndexContent(song.uID, formatted, posDB, byteSize)
    }
}