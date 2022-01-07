package modules.m2.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m2.Contact
import modules.mx.contactIndexManager
import tornadofx.Controller
import java.util.concurrent.atomic.AtomicInteger

@InternalAPI
@ExperimentalSerializationApi
class ContactIndexManager : IIndexManager, Controller() {
    override val moduleNameLong = "ContactIndexManager"
    override val module = "M2"
    override fun getIndexManager(): IIndexManager {
        return contactIndexManager!!
    }

    override var lastChangeDateHex: String = ""
    override var lastChangeDateUTC: String = ""
    override var lastChangeDateLocal: String = ""
    override var lastChangeUser: String = ""

    override var dbSizeMiByte: Double = 0.0
    override var ixSizeMiByte: Double = 0.0

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

    override suspend fun indexEntry(
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

    override fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean): String {
        return json(prettyPrint).encodeToString(entry as Contact)
    }
}
