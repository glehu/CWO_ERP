package modules.m4.logic

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m4.ItemPriceCategories
import modules.m4.ItemPriceCategory
import modules.mx.getModulePath
import java.io.File
import java.nio.file.Paths
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@InternalAPI
@ExperimentalSerializationApi
class ItemPriceManager : IModule {
  override val moduleNameLong = "ItemPriceManager"
  override val module = "M4"
  override fun getIndexManager(): IIndexManager? {
    return null
  }

  fun updateCategory(
    categoryNew: ItemPriceCategory,
    categoryOld: ItemPriceCategory
  ) {
    val categories = getCategories()
    //Check if number changed
    if (categoryNew.number != categoryOld.number) {
      categories.priceCategories.remove(categoryOld.number)
    }
    categories.priceCategories[categoryNew.number] = categoryNew
    writeCategories(categories)
  }

  fun deleteCategory(category: ItemPriceCategory) {
    val categories = getCategories()
    categories.priceCategories.remove(category.number)
    writeCategories(categories)
  }

  fun getCategories(): ItemPriceCategories {
    lateinit var priceCategories: ItemPriceCategories
    val categoryFile = getCategoriesFile()
    if (!categoryFile.isFile) initializeCategories(categoryFile)
    priceCategories = Json.decodeFromString(categoryFile.readText())
    return priceCategories
  }

  private fun writeCategories(categories: ItemPriceCategories) {
    getCategoriesFile().writeText(Json.encodeToString(categories))
  }

  private fun getCategoriesFile() = File(Paths.get(getModulePath(module), "categories.dat").toString())

  private fun initializeCategories(credentialsFile: File) {
    val mainCategory = ItemPriceCategory(0, "default", 19.0)
    credentialsFile.createNewFile()
    val categories = ItemPriceCategories(CategoryType.MAIN)
    categories.priceCategories[mainCategory.number] = mainCategory
    credentialsFile.writeText(Json.encodeToString(categories))
  }

  enum class CategoryType {
    MAIN
  }

  /**
   * Used to retrieve the first new available number.
   * @return a number to be used for a price category.
   */
  fun getNumber(categories: ItemPriceCategories): Int {
    val categoryNumber: Int
    val numbers = IntArray(categories.priceCategories.size)
    var counter = 0
    if (categories.priceCategories.isNotEmpty()) {
      for ((_, category) in categories.priceCategories) {
        numbers[counter] = category.number
        counter++
      }
      numbers.sort()
      counter = 0
      for (number in numbers) {
        if (number == counter) counter++
      }
    }
    categoryNumber = counter
    return categoryNumber
  }
}
