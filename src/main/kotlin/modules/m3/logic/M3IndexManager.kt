package modules.m3.logic

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
import modules.m3.Invoice
import modules.mx.activeUser
import modules.mx.logic.MXLog
import modules.mx.logic.indexFormat
import tornadofx.Controller

@ExperimentalSerializationApi
class M3IndexManager : IModule, IIndexManager, Controller()
{
    override fun moduleNameLong() = "M3IndexManager"
    override fun module() = "M3"
    override var module = module()
    override var moduleDescription = "Invoices"
    override var lastChangeDateHex: String = ""
    override var lastChangeDateUTC: String = ""
    override var lastChangeDateLocal: String = ""
    override var lastChangeUser: String = ""
    override val db: CwODB by inject()

    //*************************************************
    //********************** Global Data **************
    //*************************************************

    override val indexList = mutableMapOf<Int, Index>()
    override var lastUID = -1

    init
    {
        MXLog.log(module(), MXLog.LogType.INFO, "Initializing index manager...", moduleNameLong())
        indexList[0] = db.getIndex(module(), 0)
        indexList[1] = db.getIndex(module(), 1)
        indexList[2] = db.getIndex(module(), 2)
        lastUID = updateLastUID()
        getLastChangeDates()
        MXLog.log(module(), MXLog.LogType.INFO, "Index manager ready", moduleNameLong())
    }

    override fun getIndexUserSelection(): ArrayList<String>
    {
        return arrayListOf("1-Seller", "2-Buyer")
    }

    @ExperimentalSerializationApi
    override fun indexEntry(entry: Any, posDB: Long, byteSize: Int, writeToDisk: Boolean) = runBlocking {
        entry as Invoice
        buildIndex0(entry, posDB, byteSize)
        buildIndex1(entry, posDB, byteSize)
        buildIndex3(entry, posDB, byteSize)
        if (writeToDisk) launch {
            writeIndexData()
            setLastChangeData(entry.uID, activeUser)
        }
    }

    override suspend fun writeIndexData()
    {
        db.getIndexFile(module(), 0).writeText(Json.encodeToString(indexList[0]))
        db.getIndexFile(module(), 1).writeText(Json.encodeToString(indexList[1]))
        db.getIndexFile(module(), 2).writeText(Json.encodeToString(indexList[2]))
    }

    //**** **** **** **** **** INDICES **** **** **** **** ****
    //Index 0 (Invoice.uID)
    override fun buildIndex0(entry: Any, posDB: Long, byteSize: Int)
    {
        entry as Invoice
        indexList[0]!!.indexMap[entry.uID] = IndexContent(entry.uID, "${entry.uID}", posDB, byteSize)
    }

    //Index 1 (Invoice.seller)
    private fun buildIndex1(invoice: Invoice, posDB: Long, byteSize: Int)
    {
        val formatted = indexFormat(invoice.seller).uppercase()
        indexList[1]!!.indexMap[invoice.uID] = IndexContent(invoice.uID, formatted, posDB, byteSize)
    }

    //Index 2 (Invoice.buyer)
    private fun buildIndex3(invoice: Invoice, posDB: Long, byteSize: Int)
    {
        val formatted = indexFormat(invoice.buyer).uppercase()
        indexList[2]!!.indexMap[invoice.uID] = IndexContent(invoice.uID, formatted, posDB, byteSize)
    }
}