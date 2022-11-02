package modules.m7knowledge.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m7knowledge.Knowledge
import modules.mx.knowledgeIndexManager
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalSerializationApi
@InternalAPI
class KnowledgeIndexManager : IIndexManager {
  override val moduleNameLong = "KnowledgeIndexController"
  override val module = "M7"
  override fun getIndexManager(): IIndexManager {
    return knowledgeIndexManager!!
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
            2, // mainChatroomGUID,
            3 // keywords
    )
  }

  override fun getIndicesList(): ArrayList<String> {
    return arrayListOf(
            "1-GUID", "2-mainChatroomGUID", "3-keywords"
    )
  }

  override suspend fun indexEntry(
    entry: IEntry, posDB: Long, byteSize: Int, writeToDisk: Boolean, userName: String
  ) {
    entry as Knowledge
    buildIndices(
            entry.uID,
            posDB,
            byteSize,
            writeToDisk,
            userName,
            Pair(1, entry.gUID),
            Pair(2, entry.mainChatroomGUID),
            Pair(3, entry.keywords)
    )
  }

  override fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean): String {
    return json(prettyPrint).encodeToString(entry as Knowledge)
  }
}
