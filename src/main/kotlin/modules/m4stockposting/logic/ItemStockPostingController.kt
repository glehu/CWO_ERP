package modules.m4stockposting.logic

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m3.logic.InvoiceCLIController
import modules.m3.misc.AutoStorageSelectionOrderType
import modules.m4stockposting.ItemStockPosting
import modules.m4storage.logic.ItemStorageManager
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
    val ixSearchText = "<${storageUID}><${storageUnitUID}>"
    //Find stock taken from this storage unit
    getEntriesFromIndexSearch(searchText = ixSearchText, ixNr = 2, showAll = true) {
      it as ItemStockPosting
      //If finished and booked take stock away
      if (it.isFinished && it.status == 9) availableStock -= it.amount
    }
    //Find stock booked to this storage unit
    getEntriesFromIndexSearch(searchText = ixSearchText, ixNr = 3, showAll = true) {
      it as ItemStockPosting
      //If finished and booked add stock
      if (it.isFinished && it.status == 9) availableStock += it.amount
    }
    return availableStock
  }

  /**
   * Tries to find a storage unit that suits the requested amount.
   * @return a [Triple] StorageUID, StorageUnitUID and ItemStockPostingUID
   */
  fun getStorageUnitWithAtLeast(amount: Double): Triple<Int, Int, Int> {
    val entriesFound: ArrayList<ItemStockPosting> = arrayListOf()
    getEntriesFromIndexSearch(
      searchText = amount.toString(),
      ixNr = 6,
      showAll = true,
      format = false,
      numberComparison = true
    ) {
      it as ItemStockPosting
      if (check(it.storageToUID, it.storageUnitToUID, amount)) entriesFound.add(it)
    }
    //Filter the list according to the storage selection order type e.g. FIFO First In First Out and return
    return pickAccordingToOrderType(entriesFound, InvoiceCLIController().getAutoStorageSelectionOrder())
  }

  /**
   * Filters a list of stock posting entries according to the provided stock
   * @return a [Triple] StorageUID, StorageUnitUID and ItemStockPostingUID
   */
  private fun pickAccordingToOrderType(
    entriesFound: ArrayList<ItemStockPosting>,
    autoStorageSelectionOrder: AutoStorageSelectionOrderType
  ): Triple<Int, Int, Int> {
    return if (entriesFound.isEmpty()) {
      Triple(-1, -1, -1)
    } else {
      when (autoStorageSelectionOrder) {
        AutoStorageSelectionOrderType.LIFO -> getTripleOfUIDs(entriesFound.first()) //Return oldest
        AutoStorageSelectionOrderType.FIFO -> getTripleOfUIDs(entriesFound.last()) //Return newest
        else -> Triple(-1, -1, -1) //Not supported :(
      }
    }
  }

  /**
   * @return a [Triple] StorageUID, StorageUnitUID and ItemStockPostingUID
   */
  private fun getTripleOfUIDs(itemStockPosting: ItemStockPosting): Triple<Int, Int, Int> {
    return Triple(itemStockPosting.storageToUID, itemStockPosting.storageUnitToUID, itemStockPosting.uID)
  }

  fun check(storageFromUID: Int, storageUnitFromUID: Int, amount: Double? = null): Boolean {
    //Check if the storage exists
    if (storageFromUID == -1) return false
    if (storageUnitFromUID == -1) return false
    //Check details...
    val storage = ItemStorageManager().getStorages().storages[storageFromUID]!!
    //Check if storage or storage unit is locked
    if (storage.locked) return false
    if (storage.storageUnits[storageUnitFromUID].locked) return false
    //Check if the selected storage unit suits the position's amount
    if (amount != null) {
      val available = ItemStockPostingController().getAvailableStock(storageUnitFromUID, storageFromUID)
      if (available < amount) return false
    }

    return true
  }
}
