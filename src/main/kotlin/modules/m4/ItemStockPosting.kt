package modules.m4

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
    val storageUnitFromUID: Int,
    val storageUnitToUID: Int,
    val amount: Double,
    val note: String = ""
) : IEntry {
    var status: Int = 0
    var dateBooked: String = ""
    var isFinished: Boolean = false

    override fun initialize() {
        if (uID == -1) uID = itemStockPostingIndexManager!!.getUID()
        when (status) {
            !in 0..9 -> status = 0
            9 -> {
                if (dateBooked.isEmpty()) dateBooked = Timestamp.getUnixTimestampHex()
                if (!isFinished) isFinished = true
            }
        }
    }

    fun book() {
        dateBooked = Timestamp.getUnixTimestampHex()
        status = 9
        isFinished = true
    }
}
