package modules.m1.logic

import db.CwODB
import db.Index
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import modules.IDBManager
import modules.IModule
import modules.m1.Song
import java.io.RandomAccessFile

class M1DBManager : IModule, IDBManager
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
        entry.initialize()
        val songSerialized = ProtoBuf.encodeToByteArray(entry)
        val (posDBX, byteSizeX) = cwodb.saveEntry(songSerialized, entry.uID, posDB, byteSize, "M1", raf)
        indexManager.indexEntry(entry, posDBX, byteSizeX, indexWriteToDisk)
    }

    @ExperimentalSerializationApi
    override fun getEntry(uID: Int, cwodb: CwODB, index: Index): Any
    {
        return decodeEntry(cwodb.getEntryFromUniqueID(uID, "M1", index))
    }

    @ExperimentalSerializationApi
    override fun decodeEntry(entry: ByteArray): Any
    {
        return ProtoBuf.decodeFromByteArray(entry) as Song
    }
}