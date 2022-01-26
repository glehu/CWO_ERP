package modules.m4stockposting.logic

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4stockposting.ItemStockPosting
import modules.mx.itemStockPostingIndexManager

@ExperimentalSerializationApi
@InternalAPI
class ItemStockPostingController : IModule {
  override val moduleNameLong = "ItemStockPostingController"
  override val module = "M4SP"
  override fun getIndexManager(): IIndexManager {
    return itemStockPostingIndexManager!!
  }

  fun getAvailableStock(storageUnitUID: Int, storageUID: Int): Double {
    var availableStock = 0.0
    //Find stock taken from this storage unit
    getEntriesFromIndexSearch(searchText = "<${storageUID}><${storageUnitUID}>", ixNr = 2, showAll = true) {
      it as ItemStockPosting
      //If finished and booked take stock away
      if (it.isFinished && it.status == 9) availableStock -= it.amount
    }
    //Find stock booked to this storage unit
    getEntriesFromIndexSearch(searchText = "<${storageUID}><${storageUnitUID}>", ixNr = 3, showAll = true) {
      it as ItemStockPosting
      //If finished and booked add stock
      if (it.isFinished && it.status == 9) availableStock += it.amount
    }
    return availableStock
  }
}
