package modules.m2.logic

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import modules.IDBManager
import modules.IModule
import modules.m2.Contact
import modules.mx.MXLog
import java.io.RandomAccessFile

class M2DBManager : IModule, IDBManager
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