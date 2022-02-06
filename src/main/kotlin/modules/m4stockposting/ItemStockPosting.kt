package modules.m4stockposting

import interfaces.IEntry
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import modules.mx.itemStockPostingIndexManager
import modules.mx.logic.Timestamp

@InternalAPI
@ExperimentalSerializationApi
@Serializable
data class ItemStockPosting(
  override var uID: Int,
  val itemUID: Int,
  val storageFromUID: Int,
  val storageUnitFromUID: Int,
  val stockPostingFromUID: Int,
  val storageToUID: Int,
  val storageUnitToUID: Int,
  val amount: Double,
  val note: String = ""
) : IEntry {
  var stockAvailable: Double? = null
  var isFinished: Boolean = false
  var status: Int = 0

  var ixStorageAndStorageUnitFrom: String = ""
  var ixStorageAndStorageUnitTo: String = ""

  /**
   * Contains the Unix Hex Timestamp of the moment the stock posting was booked.
   */
  var dateBooked: String = ""

  override fun initialize() {
    if (uID == -1) uID = itemStockPostingIndexManager!!.getUID()
    when (status) {
      !in 0..9 -> status = 0
      9 -> {
        if (dateBooked.isEmpty()) dateBooked = Timestamp.getUnixTimestampHex()
        if (!isFinished) isFinished = true
      }
    }
    ixStorageAndStorageUnitFrom = "<${storageFromUID}><${storageUnitFromUID}>"
    ixStorageAndStorageUnitTo = "<${storageToUID}><${storageUnitToUID}>"
  }

  fun book() {
    //Do not allow multiple bookings
    if (status == 9) return
    dateBooked = Timestamp.getUnixTimestampHex()
    finalize()
    //Check null to make sure we do not set the available stock again
    if (stockAvailable == null) {
      //We only care for stock we actually own, so we return if the target storage does not exist
      if (storageToUID != -1 && storageUnitToUID != -1) {
        //... or the amount is negative
        stockAvailable = if (amount > 0) amount else 0.0
      }
    }
  }

  private fun finalize() {
    status = 9
    isFinished = true
  }
}
