package modules.m4storage.logic

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m4storage.ItemStorage
import modules.m4storage.ItemStorageUnit
import modules.m4storage.misc.ItemStorages
import modules.mx.getModulePath
import java.io.File
import java.nio.file.Paths
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@InternalAPI
@ExperimentalSerializationApi
class ItemStorageManager : IModule {
  override val moduleNameLong = "ItemStorageManager"
  override val module = "M4"
  override fun getIndexManager(): IIndexManager? {
    return null
  }

  fun funUpdateStorage(storageNew: ItemStorage, storageOld: ItemStorage) {
    val storages = getStorages()
    //Check if number changed
    if (storageNew.number != storageOld.number) {
      storages.storages.remove(storageOld.number)
    }
    storages.storages[storageNew.number] = storageNew
    writeStorages(storages)
  }

  fun deleteStorage(storage: ItemStorage) {
    val storages = getStorages()
    storages.storages.remove(storage.number)
    writeStorages(storages)
  }

  fun getStorages(): ItemStorages {
    lateinit var storages: ItemStorages
    val storageFile = getStoragesFile()
    if (!storageFile.isFile) initializeStorages(storageFile)
    storages = Json.decodeFromString(storageFile.readText())
    return storages
  }

  private fun writeStorages(categories: ItemStorages) {
    getStoragesFile().writeText(Json.encodeToString(categories))
  }

  private fun getStoragesFile() = File(Paths.get(getModulePath(module), "storages.dat").toString())

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
    val storageNumber: Int
    val numbers = IntArray(categories.storages.size)
    var counter = 0
    if (categories.storages.isNotEmpty()) {
      for ((_, category) in categories.storages) {
        numbers[counter] = category.number.toInt()
        counter++
      }
      numbers.sort()
      counter = 0
      for (number in numbers) {
        if (number == counter) counter++
      }
    }
    storageNumber = counter
    return storageNumber
  }
}
