package modules.m1.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import interfaces.IModule
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.Song
import modules.mx.m1GlobalIndex
import tornadofx.Controller
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalSerializationApi
class M1IndexManager : IModule, IIndexManager, Controller() {
    override val moduleNameLong = "M1IndexManager"
    override val module = "M1"
    override fun getIndexManager(): IIndexManager {
        return m1GlobalIndex
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
    }

    override fun indexEntry(
        entry: IEntry,
        posDB: Long,
        byteSize: Int,
        writeToDisk: Boolean,
        userName: String
    ) {
        entry as Song
        buildIndices(
            entry.uID,
            posDB,
            byteSize,
            writeToDisk,
            userName,
            Pair(1, entry.name),
            Pair(2, entry.vocalist),
            Pair(3, entry.producer),
            Pair(4, entry.genre),
            Pair(5, entry.spotifyID)
        )
    }

    override fun getIndicesList(): ArrayList<String> {
        return arrayListOf("1-Name", "2-Vocalist", "3-Producer", "4-Genre")
    }
}