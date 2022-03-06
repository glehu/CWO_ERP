package modules.m4stockposting.logic

import db.Index
import interfaces.IEntry
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import modules.m4.Item
import modules.m4stockposting.ItemStockPosting
import modules.mx.itemStockPostingIndexManager
import java.util.concurrent.atomic.AtomicInteger

@InternalAPI
@ExperimentalSerializationApi
class ItemStockPostingIndexManager : IIndexManager {
  override val moduleNameLong = "ItemStockPostingIndexManager"
  override val module = "M4SP"
  override fun getIndexManager(): IIndexManager {
    return itemStockPostingIndexManager!!
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
      1, //ItemUID
      2, //Storage Unit From
      3, //Storage Unit To
      4, //Date
      5, //Status
      6 //Amount
    )
  }

  override fun getIndicesList(): ArrayList<String> {
    return arrayListOf(
      "1-ItemUID",
      "2-From",
      "3-To",
      "4-Date",
      "5-Status",
      "6-Amount"
    )
  }

  override suspend fun indexEntry(
    entry: IEntry,
    posDB: Long,
    byteSize: Int,
    writeToDisk: Boolean,
    userName: String
  ) {
    entry as ItemStockPosting
    val stockAvailable = entry.stockAvailable ?: 0.0
    buildIndices(
      entry.uID,
      posDB,
      byteSize,
      writeToDisk,
      userName,
      Pair(1, entry.itemUID.toString()),
      Pair(2, getIndexTextIfStockAvailable(entry.ixStorageAndStorageUnitFrom, stockAvailable)),
      Pair(3, getIndexTextIfStockAvailable(entry.ixStorageAndStorageUnitTo, stockAvailable)),
      Pair(4, entry.dateBooked),
      Pair(5, entry.status.toString()),
      Pair(6, (getIndexTextIfStockAvailable(stockAvailable.toString(), stockAvailable)))
    )
  }

  override fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean): String {
    return json(prettyPrint).encodeToString(entry as Item)
  }

  private fun getIndexTextIfStockAvailable(value: String, stockAvailable: Double): String {
    return if (stockAvailable != 0.0) value else "?"
  }
}
