package modules.m4.logic

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import javafx.collections.ObservableList
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m4.M4PriceCategories
import modules.m4.M4PriceCategory
import modules.m4.gui.MG4PriceCategory
import modules.mx.getModulePath
import tornadofx.Controller
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@InternalAPI
@ExperimentalSerializationApi
class M4PriceManager : IModule, Controller() {
    override val moduleNameLong = "M4PriceManager"
    override val module = "M4"
    override fun getIndexManager(): IIndexManager? {
        return null
    }

    fun updateCategory(categoryNew: M4PriceCategory, categoryOld: M4PriceCategory, categories: M4PriceCategories) {
        //Check if username changed
        if (categoryNew.number != categoryOld.number) {
            categories.priceCategories.remove(categoryOld.number)
        }
        categories.priceCategories[categoryNew.number] = categoryNew
        writeCategories(categories)
    }

    fun deleteCategory(category: M4PriceCategory, categories: M4PriceCategories) {
        categories.priceCategories.remove(category.number)
        writeCategories(categories)
    }

    fun getCategories(): M4PriceCategories {
        val categoryFile = getCategoriesFile()
        if (!categoryFile.isFile) initializeCategories(categoryFile)
        return Json.decodeFromString(categoryFile.readText())
    }

    private fun writeCategories(categories: M4PriceCategories) {
        getCategoriesFile().writeText(Json.encodeToString(categories))
    }

    private fun getCategoriesFile() = File("${getModulePath(module)}\\categories.dat")

    private fun initializeCategories(credentialsFile: File) {
        val mainCategory = M4PriceCategory(0, "default")
        credentialsFile.createNewFile()
        val categories = M4PriceCategories(CategoryType.MAIN)
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
    private fun getNumber(categories: M4PriceCategories): Int {
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
        return counter
    }

    fun addCategory(categories: M4PriceCategories, priceCategories: ObservableList<M4PriceCategory>) =
        showCategory(M4PriceCategory(getNumber(categories), ""), categories, priceCategories)

    fun getCategories(
        priceCategories: ObservableList<M4PriceCategory>,
        categories: M4PriceCategories
    ): ObservableList<M4PriceCategory> {
        priceCategories.clear()
        val sortedMap = categories.priceCategories.toSortedMap()
        for ((_, v) in sortedMap) priceCategories.add(v)
        return priceCategories
    }

    fun showCategory(
        category: M4PriceCategory,
        categories: M4PriceCategories,
        priceCategories: ObservableList<M4PriceCategory>
    ) {
        MG4PriceCategory(category, categories).openModal(block = true)
        getCategories(priceCategories, categories)
    }
}