package modules.m1.logic

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import modules.DBManager
import modules.Module
import modules.m1.Song
import modules.m1.SongProperty
import modules.m1.getSongFromProperty
import modules.mx.MXLog
import java.io.RandomAccessFile

class M1DBManager : Module, DBManager
{
    override fun moduleName() = "M1DBManager"

    @ExperimentalSerializationApi
    override fun saveEntry(
        entry: Any, cwodb: CwODB, posDB: Long, byteSize: Int,
        raf: RandomAccessFile, indexManager: Any,
        indexWriteToDisk: Boolean
    )
    {
        entry as Song
        indexManager as M1IndexManager
        MXLog.log("M1", MXLog.LogType.INFO, "Serializing \"${entry.name}\"", moduleName())
        entry.initialize()
        val songSerialized = ProtoBuf.encodeToByteArray(entry)
        val (posDBX, byteSizeX) = cwodb.saveEntry(songSerialized, entry.uID, posDB, byteSize, "M1", raf)
        MXLog.log("M1", MXLog.LogType.INFO, "Serialization End", moduleName())
        indexManager.indexEntry(entry, posDBX, byteSizeX, indexWriteToDisk)
    }

    @ExperimentalSerializationApi
    override fun getEntry(entry: ByteArray): Any
    {
        return ProtoBuf.decodeFromByteArray(entry) as Song
    }
}