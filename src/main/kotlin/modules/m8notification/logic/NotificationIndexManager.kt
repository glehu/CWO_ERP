package modules.m8notification.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m7wisdom.Wisdom
import modules.m8notification.Notification
import modules.mx.notificationIndexManager
import java.util.concurrent.atomic.AtomicLong

@ExperimentalSerializationApi
@InternalAPI
class NotificationIndexManager(override var level: Long) : IIndexManager {
  override val moduleNameLong = "NotificationIndexController"
  override val module = "M8NOTIFICATION"
  override fun getIndexManager(): IIndexManager {
    return notificationIndexManager!!
  }

  override fun buildNewIndexManager(): IIndexManager {
    return NotificationIndexManager(level + 1)
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
            1, // GUID
            2, // recipientUsername
    )
  }

  override fun getIndicesList(): ArrayList<String> {
    return arrayListOf(
            "1-GUID", "2-recipientUsername"
    )
  }

  override suspend fun indexEntry(
    entry: IEntry, posDB: Long, byteSize: Int, writeToDisk: Boolean, userName: String
  ) {
    entry as Notification
    buildIndices(
            entry.uID, posDB, byteSize, writeToDisk, userName, Pair(1, entry.gUID), Pair(2, entry.recipientUsername)
    )
  }

  override fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean): String {
    return json(prettyPrint).encodeToString(entry as Wisdom)
  }
}
