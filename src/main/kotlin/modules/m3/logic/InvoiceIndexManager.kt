package modules.m3.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m3.Invoice
import modules.mx.invoiceIndexManager
import java.util.concurrent.atomic.AtomicLong

@InternalAPI
@ExperimentalSerializationApi
class InvoiceIndexManager(override var level: Long) : IIndexManager {
  override val moduleNameLong = "InvoiceIndexManager"
  override val module = "M3"
  override fun getIndexManager(): IIndexManager {
    return invoiceIndexManager!!
  }

  override fun buildNewIndexManager(): IIndexManager {
    return InvoiceIndexManager(level + 1)
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
            1, //Seller
            2, //Buyer
            3, //Text
            4 //Status
    )
  }

  override fun getIndicesList(): ArrayList<String> {
    return arrayListOf("1-Seller", "2-Buyer", "3-Description", "4-Status")
  }

  override suspend fun indexEntry(
    entry: IEntry,
    posDB: Long,
    byteSize: Int,
    writeToDisk: Boolean,
    userName: String
  ) {
    entry as Invoice
    buildIndices(
            entry.uID, posDB, byteSize, writeToDisk, userName, Pair(1, entry.seller), Pair(2, entry.buyer),
            Pair(3, entry.text), Pair(4, entry.status.toString()))
  }

  override fun encodeToJsonString(
    entry: IEntry,
    prettyPrint: Boolean
  ): String {
    return json(prettyPrint).encodeToString(entry as Invoice)
  }
}
