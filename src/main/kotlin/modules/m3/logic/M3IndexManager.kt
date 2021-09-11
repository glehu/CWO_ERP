package modules.m3.logic

import db.Index
import db.IndexContent
import interfaces.IIndexManager
import interfaces.IModule
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m3.Invoice
import modules.mx.logic.MXLog
import modules.mx.logic.indexFormat
import modules.mx.m3GlobalIndex
import tornadofx.Controller
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalSerializationApi
class M3IndexManager : IModule, IIndexManager, Controller() {
    override val moduleNameLong = "M3IndexManager"
    override val module = "M3"
    override fun getIndexManager(): IIndexManager {
        return m3GlobalIndex
    }

    override var lastChangeDateHex: String = ""
    override var lastChangeDateUTC: String = ""
    override var lastChangeDateLocal: String = ""
    override var lastChangeUser: String = ""

    //*************************************************
    //********************** Global Data **************
    //*************************************************

    override val indexList = mutableMapOf<Int, Index>()
    override var lastUID = AtomicInteger(-1)

    init {
        log(MXLog.LogType.INFO, "Initializing index manager...")
        indexList[0] = getIndex(0)
        indexList[1] = getIndex(1)
        indexList[2] = getIndex(2)
        indexList[3] = getIndex(3)
        lastUID = updateLastUID()
        getLastChangeDates()
        log(MXLog.LogType.INFO, "Index manager ready")
    }

    override fun getIndicesList(): ArrayList<String> {
        return arrayListOf("1-Seller", "2-Buyer", "3-Description")
    }

    override fun indexEntry(
        entry: Any,
        posDB: Long,
        byteSize: Int,
        writeToDisk: Boolean,
        userName: String
    ) = runBlocking {
        entry as Invoice
        buildIndex0(entry, posDB, byteSize)
        buildIndex1(entry, posDB, byteSize)
        buildIndex2(entry, posDB, byteSize)
        buildIndex3(entry, posDB, byteSize)
        if (writeToDisk) launch {
            writeIndexData()
        }
        setLastChangeData(entry.uID, userName)
    }

    override suspend fun writeIndexData() {
        getIndexFile(0).writeText(Json.encodeToString(indexList[0]))
        getIndexFile(1).writeText(Json.encodeToString(indexList[1]))
        getIndexFile(2).writeText(Json.encodeToString(indexList[2]))
        getIndexFile(3).writeText(Json.encodeToString(indexList[3]))
    }

    //**** **** **** **** **** INDICES **** **** **** **** ****
    //Index 0 (Invoice.uID)
    override fun buildIndex0(entry: Any, posDB: Long, byteSize: Int) {
        entry as Invoice
        indexList[0]!!.indexMap[entry.uID] = IndexContent(entry.uID, "${entry.uID}", posDB, byteSize)
    }

    //Index 1 (Invoice.seller)
    private fun buildIndex1(invoice: Invoice, posDB: Long, byteSize: Int) {
        val formatted = indexFormat(invoice.seller).uppercase()
        indexList[1]!!.indexMap[invoice.uID] = IndexContent(invoice.uID, formatted, posDB, byteSize)
    }

    //Index 2 (Invoice.buyer)
    private fun buildIndex2(invoice: Invoice, posDB: Long, byteSize: Int) {
        val formatted = indexFormat(invoice.buyer).uppercase()
        indexList[2]!!.indexMap[invoice.uID] = IndexContent(invoice.uID, formatted, posDB, byteSize)
    }

    //Index 2 (Invoice.text)
    private fun buildIndex3(invoice: Invoice, posDB: Long, byteSize: Int) {
        val formatted = indexFormat(invoice.text).uppercase()
        indexList[3]!!.indexMap[invoice.uID] = IndexContent(invoice.uID, formatted, posDB, byteSize)
    }
}