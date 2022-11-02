package modules.m5messages.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m5messages.UniMessage
import modules.mx.uniMessagesIndexManager
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalSerializationApi
@InternalAPI
class UniMessagesIndexManager : IIndexManager {
  override val moduleNameLong = "UniMessagesIndexManager"
  override val module = "M5MSG"
  override fun getIndexManager(): IIndexManager {
    return uniMessagesIndexManager!!
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
            Pair(1, if (chatUID != "-1") chatUID else "?"),
            Pair(2, entry.gUID)
    )
  }

  override fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean): String {
    return json(prettyPrint).encodeToString(entry as UniMessage)
  }
}
