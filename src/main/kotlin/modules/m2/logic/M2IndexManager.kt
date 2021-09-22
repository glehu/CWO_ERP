package modules.m2.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m2.M2Contact
import modules.mx.m2GlobalIndex
import tornadofx.Controller
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalSerializationApi
class M2IndexManager : IIndexManager, Controller() {
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
        initialize(
            1, //Name
            2, //City
            3 //SpotifyID
        )
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
        entry as M2Contact
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

    override fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean): String {
        return json(prettyPrint).encodeToString(entry as M2Contact)
    }
}