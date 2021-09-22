package modules.m4.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m4.M4Item
import modules.mx.m4GlobalIndex
import tornadofx.Controller
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalSerializationApi
class M4IndexManager : IIndexManager, Controller() {
    override val moduleNameLong = "M4IndexManager"
    override val module = "M4"
    override fun getIndexManager(): IIndexManager {
        return m4GlobalIndex
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
            1, //Description
        )
    }

    override fun getIndicesList(): ArrayList<String> {
        return arrayListOf("1-Description")
    }

    override fun indexEntry(
        entry: IEntry,
        posDB: Long,
        byteSize: Int,
        writeToDisk: Boolean,
        userName: String
    ) {
        entry as M4Item
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
        val jsonSerializer = Json {
            this.prettyPrint = prettyPrint
        }
        return jsonSerializer.encodeToString(entry as M4Item)
    }
}