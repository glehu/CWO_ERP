package modules.m4.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m4.Item
import modules.m4.ItemStockPosting
import modules.mx.m4StockPostingGlobalIndex
import tornadofx.Controller
import java.util.concurrent.atomic.AtomicInteger

@InternalAPI
@ExperimentalSerializationApi
class ItemStockPostingIndexManager : IIndexManager, Controller() {
    override val moduleNameLong = "StockPosting"
    override val module = "M4SP"
    override fun getIndexManager(): IIndexManager {
        return m4StockPostingGlobalIndex!!
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
            1, //ItemUID
            2, //Storage Unit From
            3, //Storage Unit To
            4, //Date
            5 //Status
        )
    }

    override fun getIndicesList(): ArrayList<String> {
        return arrayListOf(
            "1-ItemUID",
            "2-From",
            "3-To",
            "4-Date",
            "5-Status"
        )
    }

    override suspend fun indexEntry(
        entry: IEntry,
        posDB: Long,
        byteSize: Int,
        writeToDisk: Boolean,
        userName: String
    ) {
        entry as ItemStockPosting
        buildIndices(
            entry.uID,
            posDB,
            byteSize,
            writeToDisk,
            userName,
            Pair(1, entry.itemUID.toString()),
            Pair(2, entry.storageUnitFromUID.toString()),
            Pair(3, entry.storageUnitToUID.toString()),
            Pair(4, entry.date),
            Pair(5, entry.status.toString())
        )
    }

    override fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean): String {
        return json(prettyPrint).encodeToString(entry as Item)
    }
}
