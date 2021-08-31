package modules.m2.logic

import db.CwODB
import db.Index
import interfaces.IDBManager
import interfaces.IModule
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import modules.m2.Contact
import java.io.RandomAccessFile

@ExperimentalSerializationApi
class M2DBManager : IModule, IDBManager {
    override fun moduleNameLong() = "M2DBManager"
    override fun module() = "M2"

    override fun saveEntry(
        entry: Any, cwodb: CwODB, posDB: Long, byteSize: Int,
        raf: RandomAccessFile, indexManager: Any,
        indexWriteToDisk: Boolean,
        userName: String
    ): Int {
        entry as Contact
        indexManager as M2IndexManager
        entry.initialize()
        val songSerialized = ProtoBuf.encodeToByteArray(entry)
        val (posDBX, byteSizeX) = cwodb.saveEntry(songSerialized, entry.uID, posDB, byteSize, "M2", raf)
        indexManager.indexEntry(entry, posDBX, byteSizeX, indexWriteToDisk, userName)
        return entry.uID
    }

    override fun getEntry(uID: Int, cwodb: CwODB, index: Index): Any {
        return decodeEntry(cwodb.getEntryFromUniqueID(uID, "M2", index)) as Contact
    }

    override fun decodeEntry(entry: ByteArray): Any {
        return ProtoBuf.decodeFromByteArray(entry) as Contact
    }
}