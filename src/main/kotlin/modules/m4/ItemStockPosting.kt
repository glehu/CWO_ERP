package modules.m4

import interfaces.IEntry
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import modules.mx.logic.MXTimestamp
import modules.mx.logic.getDefaultDate
import modules.mx.m4StockPostingGlobalIndex

@InternalAPI
@ExperimentalSerializationApi
@Serializable
data class ItemStockPosting(
    override var uID: Int,
    val itemUID: Int,
    val storageUnitFromUID: Int,
    val storageUnitToUID: Int,
    val amount: Double,
    val date: String = getDefaultDate()
) : IEntry {
    var status: Int = 0
    var dateBooked: String = ""
    var isFinished: Boolean = false

    override fun initialize() {
        if (uID == -1) uID = m4StockPostingGlobalIndex!!.getUID()
        when (status) {
            !in 0..9 -> status = 0
            9 -> {
                if (!isFinished) isFinished = true
                if (dateBooked.isEmpty()) dateBooked = MXTimestamp.now()
            }
        }
    }

    fun book() {
        dateBooked = MXTimestamp.now()
        status = 9
        isFinished = true
    }
}
