package modules.m7wisdom.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m7wisdom.Wisdom
import modules.mx.wisdomIndexManager
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalSerializationApi
@InternalAPI
class WisdomIndexManager : IIndexManager {
  override val moduleNameLong = "WisdomIndexController"
  override val module = "M7WISDOM"
  override fun getIndexManager(): IIndexManager {
    return wisdomIndexManager!!
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
      1, // GUID
      2, // knowledgeUID
      3, // srcGUID
      4, // keywords
      5 // refGUID
    )
  }

  override fun getIndicesList(): ArrayList<String> {
    return arrayListOf(
      "1-GUID",
      "2-knowledgeUID",
      "3-srcWisdomUID",
      "4-keywords",
      "5-refUID"
    )
  }

  override suspend fun indexEntry(
    entry: IEntry,
    posDB: Long,
    byteSize: Int,
    writeToDisk: Boolean,
    userName: String
  ) {
    entry as Wisdom
    val knowledgeUID = entry.knowledgeUID.toString()
    val srcWisdomUID = entry.srcWisdomUID.toString()
    val refWisdomUID = entry.refWisdomUID.toString()
    buildIndices(
      entry.uID,
      posDB,
      byteSize,
      writeToDisk,
      userName,
      Pair(1, entry.gUID),
      Pair(2, if (knowledgeUID != "-1") knowledgeUID else "?"),
      Pair(3, if (srcWisdomUID != "-1") srcWisdomUID else "?"),
      Pair(4, entry.keywords),
      Pair(5, if (refWisdomUID != "-1") refWisdomUID else "?")
    )
  }

  override fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean): String {
    return json(prettyPrint).encodeToString(entry as Wisdom)
  }
}
