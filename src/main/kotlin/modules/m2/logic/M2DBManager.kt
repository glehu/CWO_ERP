package modules.m2.logic

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import modules.DBManager
import modules.Module
import modules.m2.Contact
import modules.mx.MXLog
import java.io.RandomAccessFile

class M2DBManager : Module, DBManager
{
    override fun moduleName() = "M2DBManager"
    @ExperimentalSerializationApi
    override fun saveEntry(
        entry: Any, cwodb: CwODB, posDB: Long, byteSize: Int,
        raf: RandomAccessFile, indexManager: Any,
        indexWriteToDisk: Boolean
    )
    {
        entry as Contact
        indexManager as M2IndexManager
        MXLog.log("M2", MXLog.LogType.INFO, "Serializing \"${entry.name}\"", moduleName())
        entry.initialize()
        val songSerialized = ProtoBuf.encodeToByteArray(entry)
        val (posDBX, byteSizeX) = cwodb.saveEntry(songSerialized, entry.uID, posDB, byteSize, "M2", raf)
        indexManager.indexEntry(entry, posDBX, byteSizeX, indexWriteToDisk)
    }

    @ExperimentalSerializationApi
    override fun getEntry(entry: ByteArray): Any
    {
        return ProtoBuf.decodeFromByteArray(entry) as Contact
    }
}