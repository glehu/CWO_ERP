package modules.m9process.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m9process.ProcessEvent
import modules.mx.processIndexManager
import java.util.concurrent.atomic.AtomicLong

@ExperimentalSerializationApi
@InternalAPI
class ProcessIndexManager(override var level: Long) : IIndexManager {
  override val moduleNameLong = "ProcessIndexManager"
  override val module = "M9PROCESS"
  override fun getIndexManager(): IIndexManager {
    return processIndexManager!!
  }

  override fun buildNewIndexManager(): IIndexManager {
    return ProcessIndexManager(level + 1)
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
            "1-Template", "2-Template"
    )
  }

  override suspend fun indexEntry(
    entry: IEntry, posDB: Long, byteSize: Int, writeToDisk: Boolean, userName: String
  ) {
    entry as ProcessEvent
    val chatUID = entry.guid
    buildIndices(
            entry.uID,
            posDB,
            byteSize,
            writeToDisk,
            userName,
            Pair(1, if (chatUID != "-1") chatUID else "?"),
            Pair(2, entry.guid)
    )
  }

  override fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean): String {
    return json(prettyPrint).encodeToString(entry as ProcessEvent)
  }
}
