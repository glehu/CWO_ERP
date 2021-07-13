package modules

import db.CwODB
import java.io.RandomAccessFile

interface IDBManager: IModule
{
    fun saveEntry(
        entry: Any, cwodb: CwODB, posDB: Long, byteSize: Int,
        raf: RandomAccessFile, indexManager: Any,
        indexWriteToDisk: Boolean = true
    )
    fun getEntry(entry: ByteArray): Any
}