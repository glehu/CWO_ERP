package modules.m4storage.logic

import api.logic.getTokenClient
import api.misc.json.ListDeltaJson
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import javafx.collections.ObservableList
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m4storage.ItemStorage
import modules.m4storage.ItemStorageUnit
import modules.m4storage.gui.GItemStorage
import modules.m4storage.gui.GItemStorageManager
import modules.m4storage.misc.ItemStorages
import modules.mx.cliMode
import modules.mx.getModulePath
import modules.mx.isClientGlobal
import tornadofx.Controller
import tornadofx.MultiValue
import tornadofx.observableListOf
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@InternalAPI
@ExperimentalSerializationApi
class ItemStorageManager : IModule, Controller() {
  override val moduleNameLong = "ItemStorageManager"
  override val module = "M4"
  override fun getIndexManager(): IIndexManager? {
    return null
  }

  fun funUpdateStorage(storageNew: ItemStorage, storageOld: ItemStorage) {
    if (!isClientGlobal) {
      val storages = getStorages()
      //Check if number changed
      if (storageNew.number != storageOld.number) {
        storages.storages.remove(storageOld.number)
      }
      storages.storages[storageNew.number] = storageNew
      writeStorages(storages)
    } else {
      runBlocking {
        launch {
          getTokenClient().post("${getApiUrl()}savestorage") {
            contentType(ContentType.Application.Json)
            body = ListDeltaJson(
              listEntryNew = Json.encodeToString(storageNew),
              listEntryOld = Json.encodeToString(storageOld)
            )
          }
        }
      }
    }
    if (!cliMode) find<GItemStorageManager>().refreshStorages()
  }

  fun deleteStorage(storage: ItemStorage) {
    if (!isClientGlobal) {
      val storages = getStorages()
      storages.storages.remove(storage.number)
      writeStorages(storages)
    } else {
      runBlocking {
        launch {
          getTokenClient().post("${getApiUrl()}deletestorage") {
            contentType(ContentType.Application.Json)
            body = ListDeltaJson(
              listEntryNew = Json.encodeToString(storage),
              listEntryOld = ""
            )
          }
        }
      }
    }
    if (!cliMode) tornadofx.find<GItemStorageManager>().refreshStorages()
  }

  fun getStorages(): ItemStorages {
    lateinit var storages: ItemStorages
    if (!isClientGlobal) {
      val storageFile = getStoragesFile()
      if (!storageFile.isFile) initializeStorages(storageFile)
      storages = Json.decodeFromString(storageFile.readText())
    } else {
      runBlocking {
        launch {
          storages = getTokenClient().get("${getApiUrl()}storages")
        }
      }
    }
    return storages
  }

  private fun writeStorages(categories: ItemStorages) {
    getStoragesFile().writeText(Json.encodeToString(categories))
  }

  private fun getStoragesFile() = File("${getModulePath(module)}\\storages.dat")

  private fun initializeStorages(storageFile: File) {
    val mainStorage = ItemStorage(0, "default")
    mainStorage.storageUnits.add(ItemStorageUnit(0, ""))
    val categories = ItemStorages(CategoryType.MAIN)
    categories.storages[mainStorage.number] = mainStorage
    storageFile.createNewFile()
    storageFile.writeText(Json.encodeToString(categories))
  }

  enum class CategoryType {
    MAIN
  }

  /**
   * Used to retrieve the first new available number.
   * @return a number to be used for a price category.
   */
  fun getNumber(categories: ItemStorages): Int {
    var storageNumber = 0
    if (!isClientGlobal) {
      val numbers = IntArray(categories.storages.size)
      var counter = 0
      if (categories.storages.isNotEmpty()) {
        for ((_, category) in categories.storages) {
          numbers[counter] = category.number
          counter++
        }
        numbers.sort()
        counter = 0
        for (number in numbers) {
          if (number == counter) counter++
        }
      }
      storageNumber = counter
    } else {
      runBlocking {
        launch {
          storageNumber = getTokenClient().get("${getApiUrl()}storagenumber")
        }
      }
    }
    return storageNumber
  }

  fun getLockedCellColor(isLocked: Boolean): MultiValue<Paint> =
    if (isLocked) MultiValue(arrayOf(Color.RED)) else MultiValue(arrayOf(Color.GREEN))

  fun addStorage(itemStorages: ItemStorages) =
    showItemStorageUnit(ItemStorage(getNumber(itemStorages), ""), itemStorages)

  fun getStoragesObservableList(itemStorages: ItemStorages): ObservableList<ItemStorage> {
    val itemStoragesList = observableListOf<ItemStorage>()
    val sortedMap = itemStorages.storages.toSortedMap()
    for ((_, v) in sortedMap) itemStoragesList.add(v)
    return itemStoragesList
  }

  fun showItemStorageUnit(
    itemStorage: ItemStorage,
    itemStorages: ItemStorages,
    isStorageSelectMode: Boolean = false
  ): Int {
    val storageUnit = GItemStorage(itemStorage)
    storageUnit.isStorageSelectMode = isStorageSelectMode
    storageUnit.openModal(block = true)
    val storageUnitUID = storageUnit.selectedStorageUnitUID
    getStoragesObservableList(itemStorages)
    return storageUnitUID
  }
}
