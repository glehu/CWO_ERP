package modules.m2.logic

import db.CwODB
import db.Index
import db.IndexContent
import interfaces.IIndexManager
import interfaces.IModule
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m2.Contact
import modules.mx.activeUser
import modules.mx.logic.MXLog
import tornadofx.Controller
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalSerializationApi
class M2IndexManager : IModule, IIndexManager, Controller() {
    override fun moduleNameLong() = "M2IndexManager"
    override fun module() = "M2"
    override var module = module()
    override var moduleDescription = "Contacts"
    override var lastChangeDateHex: String = ""
    override var lastChangeDateUTC: String = ""
    override var lastChangeDateLocal: String = ""
    override var lastChangeUser: String = ""
    override val db: CwODB by inject()

    //*************************************************
    //********************** Global Data **************
    //*************************************************

    override val indexList = mutableMapOf<Int, Index>()
    override var lastUID = AtomicInteger(-1)

    init {
        MXLog.log(module(), MXLog.LogType.INFO, "Initializing index manager...", moduleNameLong())
        indexList[0] = db.getIndex(module(), 0)
        indexList[1] = db.getIndex(module(), 1)
        indexList[2] = db.getIndex(module(), 2)
        indexList[3] = db.getIndex(module(), 3)
        lastUID = updateLastUID()
        getLastChangeDates()
        MXLog.log(module(), MXLog.LogType.INFO, "Index manager ready", moduleNameLong())
    }

    override fun getIndexUserSelection(): ArrayList<String> {
        return arrayListOf("1-Name", "2-City")
    }

    override fun indexEntry(entry: Any, posDB: Long, byteSize: Int, writeToDisk: Boolean) = runBlocking {
        entry as Contact
        buildIndex0(entry, posDB, byteSize)
        buildIndex1(entry, posDB, byteSize)
        buildIndex2(entry, posDB, byteSize)
        buildIndex3(entry, posDB, byteSize)
        if (writeToDisk) launch {
            writeIndexData()
        }
        setLastChangeData(entry.uID, activeUser)
    }

    override suspend fun writeIndexData() {
        db.getIndexFile(module(), 0).writeText(Json.encodeToString(indexList[0]))
        db.getIndexFile(module(), 1).writeText(Json.encodeToString(indexList[1]))
        db.getIndexFile(module(), 2).writeText(Json.encodeToString(indexList[2]))
        db.getIndexFile(module(), 3).writeText(Json.encodeToString(indexList[3]))
    }

    //**** **** **** **** **** INDICES **** **** **** **** ****
    //Index 0 (Contact.uID)
    override fun buildIndex0(entry: Any, posDB: Long, byteSize: Int) {
        entry as Contact
        indexList[0]!!.indexMap[entry.uID] = IndexContent(entry.uID, "${entry.uID}", posDB, byteSize)
    }

    //Index 1 (Contact.name)
    private fun buildIndex1(contact: Contact, posDB: Long, byteSize: Int) {
        val formatted = indexFormat(contact.name).uppercase()
        indexList[1]!!.indexMap[contact.uID] = IndexContent(contact.uID, formatted, posDB, byteSize)
    }

    //Index 2 (Contact.city)
    private fun buildIndex2(contact: Contact, posDB: Long, byteSize: Int) {
        val formatted = indexFormat(contact.city).uppercase()
        indexList[2]!!.indexMap[contact.uID] = IndexContent(contact.uID, formatted, posDB, byteSize)
    }

    //Index 3 (Contact.spotifyID)
    private fun buildIndex3(contact: Contact, posDB: Long, byteSize: Int) {
        val formatted = contact.spotifyID
        indexList[3]!!.indexMap[contact.uID] = IndexContent(contact.uID, formatted, posDB, byteSize)
    }
}