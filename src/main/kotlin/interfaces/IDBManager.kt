package interfaces

import db.CwODB
import db.Index
import java.io.RandomAccessFile

interface IDBManager : IModule {
    fun saveEntry(
        entry: Any, cwodb: CwODB, posDB: Long, byteSize: Int,
        raf: RandomAccessFile, indexManager: Any,
        indexWriteToDisk: Boolean = true
    ): Int

    fun getEntry(uID: Int, cwodb: CwODB, index: Index): Any

    fun decodeEntry(entry: ByteArray): Any
}