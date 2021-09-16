package modules.m2.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import interfaces.IModule
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.Contact
import modules.mx.m2GlobalIndex
import tornadofx.Controller
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalSerializationApi
class M2IndexManager : IModule, IIndexManager, Controller() {
    override val moduleNameLong = "M2IndexManager"
    override val module = "M2"
    override fun getIndexManager(): IIndexManager {
        return m2GlobalIndex
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
        initialize()
        getIndices(0, 1, 2, 3) //TODO
    }

    override fun getIndicesList(): ArrayList<String> {
        return arrayListOf("1-Name", "2-City")
    }

    override fun indexEntry(
        entry: IEntry,
        posDB: Long,
        byteSize: Int,
        writeToDisk: Boolean,
        userName: String
    ) {
        entry as Contact
        buildIndices(
            entry.uID,
            posDB,
            byteSize,
            writeToDisk,
            userName,
            Pair(1, entry.name),
            Pair(2, entry.city),
            Pair(3, entry.spotifyID)
        )
    }
}