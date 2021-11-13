package modules.m3.logic

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m3.M3Invoice
import modules.m3.M3InvoicePosition
import modules.m4.logic.M4PriceManager
import modules.mx.logic.roundTo

@ExperimentalSerializationApi
@InternalAPI
class M3CLIController {
    fun calculate(invoice: M3Invoice) {
        var itemPosition: M3InvoicePosition
        var pos = 0
        val categories = M4PriceManager().getCategories()
        val vat = (categories.priceCategories[invoice.priceCategory]?.vatPercent) ?: 0.0
        invoice.grossTotal = 0.0
        invoice.netTotal = 0.0
        for (item in invoice.items) {
            itemPosition = Json.decodeFromString(item.value)
            /**
             * Invoice specific calculation
             */
            invoice.grossTotal += (itemPosition.grossPrice * itemPosition.amount)

            /**
             * Line specific calculation
             */
            itemPosition.netPrice = (itemPosition.grossPrice / (1 + (vat / 100))).roundTo(2)
            invoice.items[pos] = Json.encodeToString(itemPosition)
            pos++
        }
    }
}
