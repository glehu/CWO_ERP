package modules.m2.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m2.Contact
import modules.mx.contactIndexManager
import java.util.concurrent.atomic.AtomicLong

@InternalAPI
@ExperimentalSerializationApi
class ContactIndexManager(override var level: Long) : IIndexManager {
  override val moduleNameLong = "ContactIndexManager"
  override val module = "M2"
  override fun getIndexManager(): IIndexManager {
    return contactIndexManager!!
  }

  override fun buildNewIndexManager(): IIndexManager {
    return ContactIndexManager(level + 1)
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
  override var lastUID = AtomicLong(-1L)
  override var capacity: Long = 2_000_000_000L
  override var nextManager: IIndexManager? = null
  override var prevManager: IIndexManager? = null
  override var isRemote: Boolean = false
  override var remoteURL: String = ""
  override var localMinUID: Long = -1L
  override var localMaxUID: Long = -1L

  init {
    initialize(
            1, //email
            2, //username
            3 //last-activity (for online state + no write to disk)
    )
  }

  override fun getIndicesList(): ArrayList<String> {
    return arrayListOf("1-Email", "2-Username", "3-last-activity")
  }

  override suspend fun indexEntry(
    entry: IEntry, posDB: Long, byteSize: Int, writeToDisk: Boolean, userName: String
  ) {
    entry as Contact
    buildIndices(
            entry.uID, posDB, byteSize, writeToDisk, userName, Pair(1, entry.email), Pair(2, entry.username)
    )
  }

  override fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean): String {
    return json(prettyPrint).encodeToString(entry as Contact)
  }
}
