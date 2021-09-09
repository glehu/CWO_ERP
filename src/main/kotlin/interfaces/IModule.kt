package interfaces

import db.CwODB
import db.Index
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import modules.mx.activeUser
import modules.mx.serializersModuleGlobal
import java.io.RandomAccessFile

@ExperimentalSerializationApi
interface IModule {
    fun moduleNameLong(): String
    fun module(): String
    fun getServerUrl() = "http://${modules.mx.serverIPAddressGlobal}/"
    fun getApiUrl() = "${getServerUrl()}api/${module().lowercase()}/"


    /**
     * Used to save the entry to the database.
     */
    fun save(
        entry: IEntry,
        posDB: Long = -1L,
        byteSize: Int = -1,
        raf: RandomAccessFile,
        indexManager: IIndexManager,
        indexWriteToDisk: Boolean = true,
        userName: String = activeUser.username
    ): Int {
        val protoBuf = ProtoBuf { serializersModule = serializersModuleGlobal }
        entry.initialize()
        val entrySerialized = protoBuf.encodeToByteArray(entry)
        val (posDBX, byteSizeX) = CwODB.saveEntry(
            entryBytes = entrySerialized,
            uID = entry.uID,
            posDB = posDB,
            byteSize = byteSize,
            module = indexManager.module(),
            raf = raf
        )
        indexManager.indexEntry(entry, posDBX, byteSizeX, indexWriteToDisk, userName)
        return entry.uID
    }

    /**
     * Used to retrieve an entry from the database using the provided uID.
     * It is possible to retrieve an entry of another module if that module gets passed into the function.
     * @return an entry with the provided unique identifier.
     */
    fun get(uID: Int, index: Index, module: String = module()): IEntry {
        return decode(CwODB.getEntryFromUniqueID(uID, module, index))
    }

    /**
     * @return a decoded entry from a provided ByteArray.
     */
    fun decode(entry: ByteArray): IEntry {
        val protoBuf = ProtoBuf { serializersModule = serializersModuleGlobal }
        return protoBuf.decodeFromByteArray(entry)
    }
}