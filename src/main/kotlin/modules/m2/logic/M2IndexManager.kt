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
import modules.mx.misc.indexFormat
import tornadofx.Controller
import kotlin.collections.ArrayList

@ExperimentalSerializationApi
class M2IndexManager: IModule, IIndexManager, Controller()
{
    override fun moduleName() = "M2IndexManager"
    override val indexList = mutableMapOf<Int, Index>()

    val db: CwODB by inject()

    init
    {
        MXLog.log("M2", MXLog.LogType.INFO, "Initializing index manager...", moduleName())
        indexList[0] = db.getIndex("M2", 0)
        indexList[1] = db.getIndex("M2", 1)
        MXLog.log("M2", MXLog.LogType.INFO, "Index manager ready", moduleName())
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
        db.getIndexFile("M2", 2).writeText(Json.encodeToString(indexList[2]))
        db.getIndexFile("M2", 3).writeText(Json.encodeToString(indexList[3]))
        db.getIndexFile("M2", 4).writeText(Json.encodeToString(indexList[4]))
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