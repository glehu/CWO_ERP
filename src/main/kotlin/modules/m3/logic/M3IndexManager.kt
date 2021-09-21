package modules.m3.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import interfaces.IModule
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m3.Invoice
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
        initialize(
            1, //Seller
            2, //Buyer
            3 //Text
        )
    }

    override fun getIndicesList(): ArrayList<String> {
        return arrayListOf("1-Seller", "2-Buyer", "3-Description")
    }

    override fun indexEntry(
        entry: IEntry,
        posDB: Long,
        byteSize: Int,
        writeToDisk: Boolean,
        userName: String
    ) {
        entry as Invoice
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
        val jsonSerializer = Json {
            this.prettyPrint = prettyPrint
        }
        return jsonSerializer.encodeToString(entry as Invoice)
    }
}