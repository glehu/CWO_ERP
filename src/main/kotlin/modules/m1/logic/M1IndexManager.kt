package modules.m1.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m1.M1Song
import modules.mx.m1GlobalIndex
import tornadofx.Controller
import java.util.concurrent.atomic.AtomicInteger

@InternalAPI
@ExperimentalSerializationApi
class M1IndexManager : IModule, IIndexManager, Controller() {
    override val moduleNameLong = "Discography"
    override val module = "M1"
    override fun getIndexManager(): IIndexManager {
        return m1GlobalIndex!!
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
            1, //Name
            2, //Vocalist
            3, //Producer
            4, //Genre
            5 //SpotifyID
        )
    }

    override suspend fun indexEntry(
        entry: IEntry,
        posDB: Long,
        byteSize: Int,
        writeToDisk: Boolean,
        userName: String
    ) {
        entry as M1Song
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
        return arrayListOf("1-Name", "2-Vocalist", "3-Producer", "4-Genre", "5-SpotifyID")
    }

    override fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean): String {
        return json(prettyPrint).encodeToString(entry as M1Song)
    }
}
