package modules.m7wisdom.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m7wisdom.Wisdom
import modules.mx.wisdomIndexManager
import java.util.concurrent.atomic.AtomicLong

@ExperimentalSerializationApi
@InternalAPI
class WisdomIndexManager(override var level: Long) : IIndexManager {
  override val moduleNameLong = "WisdomIndexController"
  override val module = "M7WISDOM"
  override fun getIndexManager(): IIndexManager {
    return wisdomIndexManager!!
  }

  override fun buildNewIndexManager(): IIndexManager {
    return WisdomIndexManager(level + 1)
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
            2, // knowledgeUID
            3, // srcGUID
            4, // keywords
            5, // refGUID
            6 // knowledgeUID;taskType
    )
  }

  override fun getIndicesList(): ArrayList<String> {
    return arrayListOf(
            "1-GUID", "2-knowledgeUID", "3-srcWisdomUID", "4-keywords", "5-refUID", "6-knowledgeUID;taskType")
  }

  override suspend fun indexEntry(
    entry: IEntry,
    posDB: Long,
    byteSize: Int,
    writeToDisk: Boolean,
    userName: String
  ) {
    entry as Wisdom
    val knowledgeUID = if (entry.knowledgeUID != -1L) entry.knowledgeUID.toString() else ""
    val srcWisdomUID = if (entry.srcWisdomUID != -1L) entry.srcWisdomUID.toString() else ""
    val refWisdomUID = if (entry.refWisdomUID != -1L) entry.refWisdomUID.toString() else ""
    buildIndices(
            entry.uID, posDB, byteSize, writeToDisk, userName, Pair(1, entry.guid),
            Pair(2, knowledgeUID),
            Pair(3, srcWisdomUID), Pair(4, entry.keywords.take(100)),
            Pair(5, refWisdomUID),
            Pair(6, if (entry.isTask) "$knowledgeUID;${entry.taskType}" else ""))
  }

  override fun encodeToJsonString(
    entry: IEntry,
    prettyPrint: Boolean
  ): String {
    return json(prettyPrint).encodeToString(entry as Wisdom)
  }
}
