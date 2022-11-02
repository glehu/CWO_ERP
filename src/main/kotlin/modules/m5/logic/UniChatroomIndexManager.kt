package modules.m5.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m5.UniChatroom
import modules.mx.uniChatroomIndexManager
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalSerializationApi
@InternalAPI
class UniChatroomIndexManager : IIndexManager {
  override val moduleNameLong = "UniChatroomIndexManager"
  override val module = "M5"
  override fun getIndexManager(): IIndexManager {
    return uniChatroomIndexManager!!
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
            1, //Title
            2, //ChatroomGUID
            3, //Date Created
            4, //Status
    )
  }

  override fun getIndicesList(): ArrayList<String> {
    return arrayListOf(
            "1-Title",
            "2-ChatroomGUID",
            "3-Date Created",
            "4-Status",
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
    )
  }

  override fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean): String {
    return json(prettyPrint).encodeToString(entry as UniChatroom)
  }
}
