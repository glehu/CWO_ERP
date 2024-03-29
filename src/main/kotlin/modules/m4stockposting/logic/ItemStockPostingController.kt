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

  fun getAvailableStock(
    storageUnitUID: Long,
    storageUID: Long
  ): Double {
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
  fun getStorageUnitWithAtLeast(amount: Double): Triple<Long, Long, Long> {
    val entriesFound: ArrayList<ItemStockPosting> = arrayListOf()
    getEntriesFromIndexSearch(
            searchText = amount.toString(), ixNr = 6, showAll = true, format = false, numberComparison = true) {
      it as ItemStockPosting
      if (check(
                storageFromUID = it.storageToUID, storageUnitFromUID = it.storageUnitToUID, amount = amount)) {
        entriesFound.add(it)
      }
    }
    //Filter the list according to the storage selection order type e.g. FIFO First In First Out and return
    return pickAccordingToOrderType(entriesFound, InvoiceCLIController().getAutoStorageSelectionOrder())
  }

  fun getStorageUnitsWithStock(amount: Double): Array<ArrayList<Pair<ItemStockPosting, Double>>> {
    val storagesFrom: Array<ArrayList<Pair<ItemStockPosting, Double>>> =
      arrayOf(arrayListOf(), arrayListOf(), arrayListOf())
    val entriesFound: ArrayList<ItemStockPosting> = arrayListOf()
    var amountLeft = amount
    getEntriesFromIndexSearch(
            searchText = "1", ixNr = 6, showAll = true, format = false, numberComparison = true) {
      it as ItemStockPosting
      if (check(
                storageFromUID = it.storageToUID, storageUnitFromUID = it.storageUnitToUID, amount = 1.0)) {
        entriesFound.add(it)
      }
    }
    //Evaluate and choose
    var storagesIterator = -1
    var lastStorageUID = -1L
    var lastStorageUnitUID = -1L
    when (InvoiceCLIController().getAutoStorageSelectionOrder()) {
      AutoStorageSelectionOrderType.LIFO -> {
        //Start with the oldest one
        for (stockPosting in entriesFound) {
          if (stockPosting.storageToUID != lastStorageUID || stockPosting.storageUnitToUID != lastStorageUnitUID) {
            storagesIterator++
            if (storagesIterator >= 3) break //Max storages filled, so leave
          }
          var amountTemp = stockPosting.stockAvailable
          if (amountLeft - stockPosting.stockAvailable!! <= 0) {
            amountTemp = amountLeft
            amountLeft = 0.0
          } else {
            amountLeft -= stockPosting.stockAvailable!!
          }
          storagesFrom[storagesIterator].add(Pair(stockPosting, amountTemp!!))
          lastStorageUID = stockPosting.storageToUID
          lastStorageUnitUID = stockPosting.storageUnitToUID
        }
      }

      AutoStorageSelectionOrderType.FIFO -> {
        //Start with the newest one
        for (stockPosting in entriesFound.reversed()) {
          if (stockPosting.storageToUID != lastStorageUID || stockPosting.storageUnitToUID != lastStorageUnitUID) {
            storagesIterator++
            if (storagesIterator >= 3) break //Max storages filled, so leave
          }
          var amountTemp = stockPosting.stockAvailable
          if (amountLeft - stockPosting.stockAvailable!! <= 0) {
            amountTemp = amountLeft
            amountLeft = 0.0
          } else {
            amountLeft -= stockPosting.stockAvailable!!
          }
          storagesFrom[storagesIterator].add(Pair(stockPosting, amountTemp!!))
          lastStorageUID = stockPosting.storageToUID
        }
      }

      else -> {
        return arrayOf(arrayListOf(), arrayListOf(), arrayListOf())
      }
    }
    return storagesFrom
  }

  /**
   * Filters a list of stock posting entries according to the provided stock
   * @return a [Triple] StorageUID, StorageUnitUID and ItemStockPostingUID
   */
  private fun pickAccordingToOrderType(
    entriesFound: ArrayList<ItemStockPosting>,
    autoStorageSelectionOrder: AutoStorageSelectionOrderType
  ): Triple<Long, Long, Long> {
    return if (entriesFound.isEmpty()) {
      Triple(-1L, -1L, -1L)
    } else {
      when (autoStorageSelectionOrder) {
        AutoStorageSelectionOrderType.LIFO -> getTripleOfUIDs(entriesFound.first()) //Return oldest
        AutoStorageSelectionOrderType.FIFO -> getTripleOfUIDs(entriesFound.last()) //Return newest
        else -> Triple(-1L, -1L, -1L) //Not supported :(
      }
    }
  }

  /**
   * @return a [Triple] StorageUID, StorageUnitUID and ItemStockPostingUID
   */
  private fun getTripleOfUIDs(itemStockPosting: ItemStockPosting): Triple<Long, Long, Long> {
    return Triple(itemStockPosting.storageToUID, itemStockPosting.storageUnitToUID, itemStockPosting.uID)
  }

  fun check(
    storageFromUID: Long,
    storageUnitFromUID: Long,
    amount: Double? = null
  ): Boolean {
    //Check if the storage exists
    if (storageFromUID == -1L) return false
    if (storageUnitFromUID == -1L) return false
    //Check details...
    val storage = ItemStorageManager().getStorages().storages[storageFromUID]!!
    //Check if storage or storage unit is locked
    if (storage.locked) return false
    if (storage.storageUnits[storageUnitFromUID.toInt()].locked) return false
    //Check if the selected storage unit suits the position's amount
    if (amount != null) {
      val available = ItemStockPostingController().getAvailableStock(storageUnitFromUID, storageFromUID)
      if (available < amount) return false
    }
    return true
  }
}
