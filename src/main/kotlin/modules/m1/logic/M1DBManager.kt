package modules.m1.logic

import db.CwODB
import db.Index
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import modules.interfaces.IDBManager
import modules.interfaces.IModule
import modules.m1.Song
import java.io.RandomAccessFile

class M1DBManager : IModule, IDBManager
{
    override fun moduleNameLong() = "M1DBManager"
    override fun module() = "M1"

    @ExperimentalSerializationApi
    override fun saveEntry(
        entry: Any, cwodb: CwODB, posDB: Long, byteSize: Int,
        raf: RandomAccessFile, indexManager: Any,
        indexWriteToDisk: Boolean
    ): Int
    {
        entry as Song
        indexManager as M1IndexManager
        entry.initialize()
        val songSerialized = ProtoBuf.encodeToByteArray(entry)
        val (posDBX, byteSizeX) = cwodb.saveEntry(songSerialized, entry.uID, posDB, byteSize, module(), raf)
        indexManager.indexEntry(entry, posDBX, byteSizeX, indexWriteToDisk)
        return entry.uID
    }

    @ExperimentalSerializationApi
    override fun getEntry(uID: Int, cwodb: CwODB, index: Index): Any
    {
        return decodeEntry(cwodb.getEntryFromUniqueID(uID, module(), index)) as Song
    }

    @ExperimentalSerializationApi
    override fun decodeEntry(entry: ByteArray): Any
    {
        return ProtoBuf.decodeFromByteArray(entry) as Song
    }
}