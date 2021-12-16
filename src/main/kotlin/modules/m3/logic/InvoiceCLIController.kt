package modules.m3.logic

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m3.Invoice
import modules.m3.InvoicePosition
import modules.m3.misc.InvoiceIni
import modules.m4.logic.ItemPriceManager
import modules.mx.logic.roundTo
import modules.mx.invoiceIndexManager

@ExperimentalSerializationApi
@InternalAPI
class InvoiceCLIController : IModule {
    override val moduleNameLong = "InvoiceCLIController"
    override val module = "M3"
    override fun getIndexManager(): IIndexManager {
        return invoiceIndexManager!!
    }

    fun calculate(invoice: Invoice) {
        var itemPosition: InvoicePosition
        var pos = 0
        val categories = ItemPriceManager().getCategories()
        val vat = (categories.priceCategories[invoice.priceCategory]?.vatPercent) ?: 0.0
        invoice.grossTotal = 0.0
        invoice.netTotal = 0.0
        for (item in invoice.items) {
            itemPosition = Json.decodeFromString(item.value)
            //Invoice specific calculation
            invoice.grossTotal += (itemPosition.grossPrice * itemPosition.amount)
            //Line specific calculation
            itemPosition.netPrice = (itemPosition.grossPrice / (1 + (vat / 100))).roundTo(2)
            invoice.items[pos] = Json.encodeToString(itemPosition)
            pos++
        }
    }

    fun getIni(): InvoiceIni {
        val iniTxt = getSettingsFileText()
        return if (iniTxt.isNotEmpty()) Json.decodeFromString(iniTxt) else InvoiceIni()
    }

    fun getStatusText(status: Int): String {
        return getIni().statusTexts[status] ?: "?"
    }
}
