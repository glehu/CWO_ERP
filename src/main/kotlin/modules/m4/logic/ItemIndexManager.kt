package modules.m4.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m4.Item
import modules.mx.itemIndexManager
import tornadofx.Controller
import java.util.concurrent.atomic.AtomicInteger

@InternalAPI
@ExperimentalSerializationApi
class ItemIndexManager : IIndexManager, Controller() {
    override val moduleNameLong = "ItemIndexManager"
    override val module = "M4"
    override fun getIndexManager(): IIndexManager {
        return itemIndexManager!!
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
            1, //Description
        )
    }

    override fun getIndicesList(): ArrayList<String> {
        return arrayListOf("1-Description")
    }

    override suspend fun indexEntry(
        entry: IEntry,
        posDB: Long,
        byteSize: Int,
        writeToDisk: Boolean,
        userName: String
    ) {
        entry as Item
        buildIndices(
            entry.uID,
            posDB,
            byteSize,
            writeToDisk,
            userName,
            Pair(1, entry.description),
        )
    }

    override fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean): String {
        return json(prettyPrint).encodeToString(entry as Item)
    }
}