package modules

import db.CwODB
import db.Index
import javafx.beans.property.SimpleIntegerProperty
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

interface IIndexManager : IModule
{
    val db: CwODB
    val indexList: Map<Int, Index>
    var lastUID: Int
    var module: String
    var moduleDescription: String
    fun getUID(): Int
    {
        lastUID++
        runBlocking { launch { db.setLastUniqueID(lastUID, module()) } }
        return lastUID
    }

    fun updateLastUID() = db.getLastUniqueID(module())

    fun getIndexUserSelection(): ArrayList<String>
    fun indexEntry(entry: Any, posDB: Long, byteSize: Int, writeToDisk: Boolean = true)
    fun buildIndex0(entry: Any, posDB: Long, byteSize: Int)
    suspend fun writeIndexData()
}