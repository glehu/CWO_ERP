package modules.m3.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m3.Invoice
import modules.mx.invoiceIndexManager
import java.util.concurrent.atomic.AtomicInteger

@InternalAPI
@ExperimentalSerializationApi
class InvoiceIndexManager : IIndexManager {
  override val moduleNameLong = "InvoiceIndexManager"
  override val module = "M3"
  override fun getIndexManager(): IIndexManager {
    return invoiceIndexManager!!
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
    entry: IEntry, posDB: Long, byteSize: Int, writeToDisk: Boolean, userName: String
  ) {
    entry as Invoice
    buildIndices(
            entry.uID,
            posDB,
            byteSize,
            writeToDisk,
            userName,
            Pair(1, entry.seller),
            Pair(2, entry.buyer),
            Pair(3, entry.text),
            Pair(4, entry.status.toString())
    )
  }

  override fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean): String {
    return json(prettyPrint).encodeToString(entry as Invoice)
  }
}
