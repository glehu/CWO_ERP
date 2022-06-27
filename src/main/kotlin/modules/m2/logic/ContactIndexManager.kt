package modules.m2.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m2.Contact
import modules.mx.contactIndexManager
import java.util.concurrent.atomic.AtomicInteger

@InternalAPI
@ExperimentalSerializationApi
class ContactIndexManager : IIndexManager {
  override val moduleNameLong = "ContactIndexManager"
  override val module = "M2"
  override fun getIndexManager(): IIndexManager {
    return contactIndexManager!!
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
      1, //email
    )
  }

  override fun getIndicesList(): ArrayList<String> {
    return arrayListOf("1-Email")
  }

  override suspend fun indexEntry(
    entry: IEntry,
    posDB: Long,
    byteSize: Int,
    writeToDisk: Boolean,
    userName: String
  ) {
    entry as Contact
    buildIndices(
      entry.uID,
      posDB,
      byteSize,
      writeToDisk,
      userName,
      Pair(1, entry.email)
    )
  }

  override fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean): String {
    return json(prettyPrint).encodeToString(entry as Contact)
  }
}
