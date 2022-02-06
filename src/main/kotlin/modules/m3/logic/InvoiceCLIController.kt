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
import modules.m3.misc.AutoStorageSelectionOrderType
import modules.m3.misc.InvoiceIni
import modules.m4.logic.ItemPriceManager
import modules.mx.invoiceIndexManager
import modules.mx.logic.roundTo

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
      itemPosition.netPrice = getNetFromGross(itemPosition.grossPrice, vat)
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

  fun getNetFromGross(gross: Double, vat: Double): Double {
    return (gross / (1 + (vat / 100))).roundTo(2)
  }

  fun getGrossFromNet(net: Double, vat: Double): Double {
    return net + (net * vat)
  }

  /**
   * @return [AutoStorageSelectionOrderType]
   */
  fun getAutoStorageSelectionOrder(invoiceIni: InvoiceIni? = null): AutoStorageSelectionOrderType {
    val ini = invoiceIni ?: getIni()
    return when (ini.autoStorageSelectionOrder) {
      "LIFO" -> AutoStorageSelectionOrderType.LIFO
      "FIFO" -> AutoStorageSelectionOrderType.FIFO
      "HIFO" -> AutoStorageSelectionOrderType.HIFO
      "LOFO" -> AutoStorageSelectionOrderType.LOFO
      "FEFO" -> AutoStorageSelectionOrderType.FEFO
      else -> AutoStorageSelectionOrderType.LIFO
    }
  }
}
