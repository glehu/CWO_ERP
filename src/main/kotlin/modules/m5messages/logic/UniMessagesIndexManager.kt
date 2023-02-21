package modules.m5messages.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m5messages.UniMessage
import modules.mx.uniMessagesIndexManager
import java.util.concurrent.atomic.AtomicLong

@ExperimentalSerializationApi
@InternalAPI
class UniMessagesIndexManager(override var level: Long) : IIndexManager {
  override val moduleNameLong = "UniMessagesIndexManager"
  override val module = "M5MSG"
  override fun getIndexManager(): IIndexManager {
    return uniMessagesIndexManager!!
  }

  override fun buildNewIndexManager(): IIndexManager {
    return UniMessagesIndexManager(level + 1)
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
            1, // ChatroomUID
            2 // GUID
    )
  }

  override fun getIndicesList(): ArrayList<String> {
    return arrayListOf(
            "1-ChatroomUID", "2-GUID"
    )
  }

  override suspend fun indexEntry(
    entry: IEntry, posDB: Long, byteSize: Int, writeToDisk: Boolean, userName: String
  ) {
    entry as UniMessage
    val chatUID = entry.uniChatroomUID.toString()
    buildIndices(
            entry.uID,
            posDB,
            byteSize,
            writeToDisk,
            userName,
            Pair(1, if (chatUID != "-1") chatUID else ""),
            Pair(2, entry.guid)
    )
  }

  override fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean): String {
    return json(prettyPrint).encodeToString(entry as UniMessage)
  }
}
