package modules.m2.logic

import db.CwODB
import db.Index
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import modules.IDBManager
import modules.IModule
import modules.m2.Contact
import java.io.RandomAccessFile

class M2DBManager : IModule, IDBManager
{
    override fun moduleNameLong() = "M2DBManager"
    override fun module() = "M2"

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
    override fun getEntry(uID: Int, cwodb: CwODB, index: Index): Any
    {
        return decodeEntry(cwodb.getEntryFromUniqueID(uID, "M2", index)) as Contact
    }

    @ExperimentalSerializationApi
    override fun decodeEntry(entry: ByteArray): Any
    {
        return ProtoBuf.decodeFromByteArray(entry) as Contact
    }
}