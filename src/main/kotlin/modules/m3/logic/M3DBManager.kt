package modules.m3.logic

import db.CwODB
import db.Index
import interfaces.IDBManager
import interfaces.IModule
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import modules.m3.Invoice
import java.io.RandomAccessFile

@ExperimentalSerializationApi
class M3DBManager : IModule, IDBManager {
    override fun moduleNameLong() = "M3DBManager"
    override fun module() = "M3"

    override fun saveEntry(
        entry: Any, cwodb: CwODB, posDB: Long, byteSize: Int,
        raf: RandomAccessFile, indexManager: Any,
        indexWriteToDisk: Boolean,
        userName: String
    ): Int {
        entry as Invoice
        indexManager as M3IndexManager
        entry.initialize()
        val songSerialized = ProtoBuf.encodeToByteArray(entry)
        val (posDBX, byteSizeX) = cwodb.saveEntry(songSerialized, entry.uID, posDB, byteSize, module(), raf)
        indexManager.indexEntry(entry, posDBX, byteSizeX, indexWriteToDisk, userName)
        return entry.uID
    }

    override fun getEntry(uID: Int, cwodb: CwODB, index: Index): Any {
        return decodeEntry(cwodb.getEntryFromUniqueID(uID, module(), index)) as Invoice
    }

    override fun decodeEntry(entry: ByteArray): Any {
        return ProtoBuf.decodeFromByteArray(entry) as Invoice
    }
}