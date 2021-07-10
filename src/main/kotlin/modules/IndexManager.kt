package modules

import db.Index

interface IndexManager : Module
{
    val indexList: Map<Int, Index>
    fun getIndexUserSelection(): ArrayList<String>
    fun indexEntry(entry: Any, posDB: Long, byteSize: Int, writeToDisk: Boolean = true)
    fun buildIndex0(entry: Any, posDB: Long, byteSize: Int)
    fun writeIndexData()
}