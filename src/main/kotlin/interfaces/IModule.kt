package interfaces

import db.CwODB
import db.Index
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import modules.mx.activeUser
import modules.mx.protoBufGlobal
import java.io.RandomAccessFile

@ExperimentalSerializationApi
interface IModule {
    val moduleNameLong: String
    val module: String
    fun getServerUrl() = "http://${modules.mx.serverIPAddressGlobal}/"
    fun getApiUrl() = "${getServerUrl()}api/${module.lowercase()}/"


    /**
     * Used to save the entry to the database.
     */
    fun save(
        entry: IEntry,
        raf: RandomAccessFile,
        indexManager: IIndexManager,
        indexWriteToDisk: Boolean = true,
        userName: String = activeUser.username
    ): Int {
        val posDB: Long
        val byteSize: Int
        if (entry.uID != -1) {
            val index = indexManager.indexList[0]!!.indexMap[entry.uID]
            if (index != null) {
                posDB = index.pos
                byteSize = index.byteSize
            } else {
                posDB = -1L
                byteSize = -1
            }
        } else {
            posDB = -1L
            byteSize = -1
        }
        entry.initialize()
        val entrySerialized = protoBufGlobal.encodeToByteArray(entry)
        val (posDBX, byteSizeX) = CwODB.saveEntry(
            entryBytes = entrySerialized,
            uID = entry.uID,
            posDB = posDB,
            byteSize = byteSize,
            module = indexManager.module,
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
    fun get(uID: Int, index: Index, module: String = this.module): IEntry {
        return decode(CwODB.getEntryFromUniqueID(uID, module, index))
    }

    /**
     * @return a decoded entry from a provided ByteArray.
     */
    fun decode(entry: ByteArray): IEntry {
        return protoBufGlobal.decodeFromByteArray(entry)
    }
}