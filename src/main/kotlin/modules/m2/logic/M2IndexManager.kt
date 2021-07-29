package modules.m2.logic

import db.CwODB
import db.Index
import db.IndexContent
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.IIndexManager
import modules.IModule
import modules.m2.Contact
import modules.mx.logic.MXLog
import modules.mx.logic.indexFormat
import tornadofx.Controller

@ExperimentalSerializationApi
class M2IndexManager : IModule, IIndexManager, Controller()
{
    override fun moduleNameLong() = "M2IndexManager"
    override fun module() = "M2"

    override val indexList = mutableMapOf<Int, Index>()
    override var lastUID = -1

    override val db: CwODB by inject()

    init
    {
        MXLog.log("M2", MXLog.LogType.INFO, "Initializing index manager...", moduleNameLong())
        indexList[0] = db.getIndex("M2", 0)
        indexList[1] = db.getIndex("M2", 1)
        MXLog.log("M2", MXLog.LogType.INFO, "Index manager ready", moduleNameLong())
    }

    override fun getIndexUserSelection(): ArrayList<String>
    {
        return arrayListOf("1-Name")
    }

    @ExperimentalSerializationApi
    override fun indexEntry(entry: Any, posDB: Long, byteSize: Int, writeToDisk: Boolean) = runBlocking {
        entry as Contact
        buildIndex0(entry, posDB, byteSize)
        buildIndex1(entry, posDB, byteSize)
        if (writeToDisk) launch { writeIndexData() }
    }

    override suspend fun writeIndexData()
    {
        db.getIndexFile("M2", 0).writeText(Json.encodeToString(indexList[0]))
        db.getIndexFile("M2", 1).writeText(Json.encodeToString(indexList[1]))
    }

    //**** **** **** **** **** INDICES **** **** **** **** ****
    //Index 0 (Contact.uID)
    override fun buildIndex0(entry: Any, posDB: Long, byteSize: Int)
    {
        entry as Contact
        indexList[0]!!.indexMap[entry.uID] = IndexContent(entry.uID, "${entry.uID}", posDB, byteSize)
    }

    //Index 1 (Contact.name)
    private fun buildIndex1(contact: Contact, posDB: Long, byteSize: Int)
    {
        val formatted = indexFormat(contact.name).uppercase()
        indexList[1]!!.indexMap[contact.uID] = IndexContent(contact.uID, formatted, posDB, byteSize)
    }
}