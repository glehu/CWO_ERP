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
            1, // GUID
            2, // Mode|KnowledgeUID
            3, // WisdomUID
            4 // TaskWisdomUID
    )
  }

  override fun getIndicesList(): ArrayList<String> {
    return arrayListOf("1-GUID", "2-Mode|KnowledgeUID", "3-WisdomUID", "4-TaskWisdomUID")
  }

  override suspend fun indexEntry(
    entry: IEntry,
    posDB: Long,
    byteSize: Int,
    writeToDisk: Boolean,
    userName: String
  ) {
    entry as ProcessEvent
    val wisdomUID = entry.wisdomUID
    val taskWisdomUID = entry.taskWisdomUID
    var index2 = entry.mode + "|" + entry.knowledgeUID.toString()
    // Check if both mode and knowledgeUID are invalid (e.g. deleted entry)
    // The value would be an empty mode () and minus one (-1) seperated by a pipe (|) thus "|-1"
    if (index2 == "|-1") index2 = ""
    buildIndices(
            entry.uID, posDB, byteSize, writeToDisk, userName, Pair(1, entry.guid), Pair(2, index2),
            Pair(3, if (wisdomUID != -1L) wisdomUID.toString() else ""),
            Pair(4, if (taskWisdomUID != -1L) taskWisdomUID.toString() else ""))
  }

  override fun encodeToJsonString(
    entry: IEntry,
    prettyPrint: Boolean
  ): String {
    return json(prettyPrint).encodeToString(entry as ProcessEvent)
  }
}
