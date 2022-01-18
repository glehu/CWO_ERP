package modules.m4.logic

import interfaces.IController
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.misc.SongPropertyMainDataModel
import modules.m4.Item
import modules.m4.gui.GItemFinder
import modules.m4.gui.GItemSettings
import modules.m4.gui.ItemConfiguratorWizard
import modules.m4.misc.ItemProperty
import modules.m4.misc.getItemFromItemProperty
import modules.m4.misc.getItemPropertyFromItem
import modules.m4stockposting.ItemStockPosting
import modules.m4stockposting.logic.ItemStockPostingController
import modules.mx.itemIndexManager
import tornadofx.Controller
import tornadofx.Scope

@InternalAPI
@ExperimentalSerializationApi
class ItemController : IController, Controller() {
  override val moduleNameLong = "ItemController"
  override val module = "M4"
  override fun getIndexManager(): IIndexManager {
    return itemIndexManager!!
  }

  private val wizard = find<ItemConfiguratorWizard>()

  override fun searchEntry() {
    find<GItemFinder>().openModal()
  }

  override fun newEntry() {
    wizard.item.commit()
    if (wizard.item.isValid && wizard.item.uID.value != -1) {
      setEntryLock(wizard.item.uID.value, false)
    }
    wizard.item.priceCategories.value.clear()
    wizard.item.storages.value.clear()
    wizard.item.item = ItemProperty()
    wizard.item.validate()
    wizard.isComplete = false
  }

  override suspend fun saveEntry(unlock: Boolean) {
    if (wizard.item.isValid) {
      wizard.item.commit()
      wizard.item.uID.value = save(
        entry = getItemFromItemProperty(wizard.item.item),
        unlock = unlock
      )
      wizard.isComplete = false
    }
  }

  override fun showEntry(uID: Int) {
    val entry = get(uID) as Item
    wizard.item.item = getItemPropertyFromItem(entry)
  }

  fun createAndReturnItem(): Item {
    val item = Item(-1, "")
    val wizard = ItemConfiguratorWizard()
    wizard.showHeader = false
    wizard.showSteps = false
    wizard.item.item = getItemPropertyFromItem(item)
    wizard.openModal(block = true)
    return getItemFromItemProperty(wizard.item.item)
  }

  fun selectAndReturnItem(): Item {
    val item: Item
    val newScope = Scope()
    val dataTransfer = SongPropertyMainDataModel()
    dataTransfer.uID.value = -2
    setInScope(dataTransfer, newScope)
    tornadofx.find<GItemFinder>(newScope).openModal(block = true)
    item = if (dataTransfer.name.value != null) {
      load(dataTransfer.uID.value) as Item
    } else Item(-1, "")
    return item
  }

  fun showSettings() {
    val settings = find<GItemSettings>()
    settings.openModal()
  }

  fun addStock(storageUID: Int, storageUnitUID: Int, amount: Double, note: String = "") {
    wizard.item
      .storages.value[storageUID]
      .storageUnits[storageUnitUID].stock += amount
    postStock(wizard.item.uID.value, storageUID, storageUnitUID, amount, note)
    runBlocking {
      ItemController().saveEntry(unlock = false)
    }
  }

  fun postStock(itemUID: Int, storageUID: Int, storageUnitUID: Int, amount: Double, note: String = "") {
    val stockPosting = ItemStockPosting(
      uID = -1,
      itemUID = itemUID,
      storageFromUID = -1,
      storageUnitFromUID = -1,
      storageToUID = storageUID,
      storageUnitToUID = storageUnitUID,
      amount = amount,
      note = note
    )
    stockPosting.book()
    runBlocking {
      ItemStockPostingController().save(stockPosting)
    }
  }
}
