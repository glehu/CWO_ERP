package modules

import db.CwODB
import modules.m1.Song
import modules.m1.logic.M1IndexManager
import java.io.RandomAccessFile

interface DBManager: Module
{
    fun saveEntry(
        entry: Any, cwodb: CwODB, posDB: Long, byteSize: Int,
        raf: RandomAccessFile, indexManager: Any,
        indexWriteToDisk: Boolean = true
    )
    fun getEntry(entry: ByteArray): Any
}