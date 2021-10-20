package modules.m3Invoices.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m3Invoices.M3Invoice
import modules.mx.m3GlobalIndex
import tornadofx.Controller
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalSerializationApi
class M3IndexManager : IIndexManager, Controller() {
    override val moduleNameLong = "Invoices"
    override val module = "M3"
    override fun getIndexManager(): IIndexManager {
        return m3GlobalIndex!!
    }

    override var lastChangeDateHex: String = ""
    override var lastChangeDateUTC: String = ""
    override var lastChangeDateLocal: String = ""
    override var lastChangeUser: String = ""

    override var dbSizeKiByte: Double = 0.0
    override var ixSizeKiByte: Double = 0.0

    //*************************************************
    //********************** Global Data **************
    //*************************************************

    override val indexList = mutableMapOf<Int, Index>()
    override var lastUID = AtomicInteger(-1)

    init {
        initialize(
            1, //Seller
            2, //Buyer
            3 //Text
        )
    }

    override fun getIndicesList(): ArrayList<String> {
        return arrayListOf("1-Seller", "2-Buyer", "3-Description")
    }

    override suspend fun indexEntry(
        entry: IEntry,
        posDB: Long,
        byteSize: Int,
        writeToDisk: Boolean,
        userName: String
    ) {
        entry as M3Invoice
        buildIndices(
            entry.uID,
            posDB,
            byteSize,
            writeToDisk,
            userName,
            Pair(1, entry.seller),
            Pair(2, entry.buyer),
            Pair(3, entry.text),
        )
    }

    override fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean): String {
        return json(prettyPrint).encodeToString(entry as M3Invoice)
    }
}
