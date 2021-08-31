package interfaces

import db.CwODB
import db.Index
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.RandomAccessFile

@ExperimentalSerializationApi
interface IDBManager : IModule {
    /**
     * Saves an entry to the database.
     */
    fun saveEntry(
        entry: Any, cwodb: CwODB, posDB: Long, byteSize: Int,
        raf: RandomAccessFile, indexManager: Any,
        indexWriteToDisk: Boolean = true
    ): Int

    /**
     * @return an entry with the provided unique identifier.
     */
    fun getEntry(uID: Int, cwodb: CwODB, index: Index): Any

    /**
     * @return a decoded entry from a provided ByteArray.
     */
    fun decodeEntry(entry: ByteArray): Any
}