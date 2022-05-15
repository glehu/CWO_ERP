package modules.m6.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m4.Item
import modules.m6.Snippet
import modules.mx.snippetBaseIndexManager
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalSerializationApi
@InternalAPI
class SnippetBaseIndexManager : IIndexManager {
  override val moduleNameLong = "SnippetBaseIndexManager"
  override val module = "M6"
  override fun getIndexManager(): IIndexManager {
    return snippetBaseIndexManager!!
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
      1, //ChatroomGUID
    )
  }

  override fun getIndicesList(): ArrayList<String> {
    return arrayListOf(
      "1-ChatroomGUID",
    )
  }

  override suspend fun indexEntry(
    entry: IEntry,
    posDB: Long,
    byteSize: Int,
    writeToDisk: Boolean,
    userName: String
  ) {
    entry as Snippet
    buildIndices(
      entry.uID,
      posDB,
      byteSize,
      writeToDisk,
      userName,
      Pair(1, entry.gUID)
    )
  }

  override fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean): String {
    return json(prettyPrint).encodeToString(entry as Item)
  }
}
