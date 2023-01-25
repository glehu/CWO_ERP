package modules.m5.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m5.UniChatroom
import modules.mx.uniChatroomIndexManager
import java.util.concurrent.atomic.AtomicLong

@ExperimentalSerializationApi
@InternalAPI
class UniChatroomIndexManager(override var level: Long) : IIndexManager {
  override val moduleNameLong = "UniChatroomIndexManager"
  override val module = "M5"
  override fun getIndexManager(): IIndexManager {
    return uniChatroomIndexManager!!
  }

  override fun buildNewIndexManager(): IIndexManager {
    return UniChatroomIndexManager(level + 1)
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
            1, // Title
            2, // ChatroomGUID
            3, // Date Created
            4, // Status
            5 // DirectMessageUsername
    )
  }

  override fun getIndicesList(): ArrayList<String> {
    return arrayListOf(
            "1-Title", "2-ChatroomGUID", "3-Date Created", "4-Status", "5-DirectMessageUsername"
    )
  }

  override suspend fun indexEntry(
    entry: IEntry, posDB: Long, byteSize: Int, writeToDisk: Boolean, userName: String
  ) {
    entry as UniChatroom
    buildIndices(
            entry.uID,
            posDB,
            byteSize,
            writeToDisk,
            userName,
            Pair(1, entry.title),
            Pair(2, entry.chatroomGUID),
            Pair(3, entry.dateCreated),
            Pair(4, entry.status.toString()),
            Pair(5, entry.directMessageUsername)
    )
  }

  override fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean): String {
    return json(prettyPrint).encodeToString(entry as UniChatroom)
  }
}
