package modules.m4.logic

import api.logic.getTokenClient
import api.misc.json.ListDeltaJson
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import javafx.collections.ObservableList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m4.M4PriceCategories
import modules.m4.M4PriceCategory
import modules.m4.gui.MG4PriceCategory
import modules.m4.gui.MG4PriceManager
import modules.mx.cliMode
import modules.mx.getModulePath
import modules.mx.isClientGlobal
import tornadofx.Controller
import tornadofx.observableListOf
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

    fun updateCategory(categoryNew: M4PriceCategory, categoryOld: M4PriceCategory) {
        if (!isClientGlobal) {
            val categories = getCategories()
            //Check if number changed
            if (categoryNew.number != categoryOld.number) {
                categories.priceCategories.remove(categoryOld.number)
            }
            categories.priceCategories[categoryNew.number] = categoryNew
            writeCategories(categories)
        } else {
            runBlocking {
                launch {
                    getTokenClient().post("${getApiUrl()}savecategory") {
                        contentType(ContentType.Application.Json)
                        body = ListDeltaJson(
                            listEntryNew = Json.encodeToString(categoryNew),
                            listEntryOld = Json.encodeToString(categoryOld)
                        )
                    }
                }
            }
        }
        if (!cliMode) find<MG4PriceManager>().refreshCategories()
    }

    fun deleteCategory(category: M4PriceCategory) {
        if (!isClientGlobal) {
            val categories = getCategories()
            categories.priceCategories.remove(category.number)
            writeCategories(categories)
        } else {
            runBlocking {
                launch {
                    getTokenClient().post("${getApiUrl()}deletecategory") {
                        contentType(ContentType.Application.Json)
                        body = ListDeltaJson(
                            listEntryNew = Json.encodeToString(category),
                            listEntryOld = ""
                        )
                    }
                }
            }
        }
        if (!cliMode) tornadofx.find<MG4PriceManager>().refreshCategories()
    }

    fun getCategories(): M4PriceCategories {
        lateinit var priceCategories: M4PriceCategories
        if (!isClientGlobal) {
            val categoryFile = getCategoriesFile()
            if (!categoryFile.isFile) initializeCategories(categoryFile)
            priceCategories = Json.decodeFromString(categoryFile.readText())
        } else {
            runBlocking {
                launch {
                    priceCategories = getTokenClient().get("${getApiUrl()}pricecategories")
                }
            }
        }
        return priceCategories
    }

    private fun writeCategories(categories: M4PriceCategories) {
        getCategoriesFile().writeText(Json.encodeToString(categories))
    }

    private fun getCategoriesFile() = File("${getModulePath(module)}\\categories.dat")

    private fun initializeCategories(credentialsFile: File) {
        val mainCategory = M4PriceCategory(0, "default", 19.0)
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
    fun getNumber(categories: M4PriceCategories): Int {
        var categoryNumber = 0
        if (!isClientGlobal) {
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
        } else {
            runBlocking {
                launch {
                    categoryNumber = getTokenClient().get("${getApiUrl()}categorynumber")
                }
            }
        }
        return categoryNumber
    }

    fun addCategory(categories: M4PriceCategories) =
        showCategory(M4PriceCategory(getNumber(categories), "", 19.0), categories)

    fun getCategories(categories: M4PriceCategories): ObservableList<M4PriceCategory> {
        val priceCategories = observableListOf<M4PriceCategory>()
        val sortedMap = categories.priceCategories.toSortedMap()
        for ((_, v) in sortedMap) priceCategories.add(v)
        return priceCategories
    }

    fun showCategory(category: M4PriceCategory, categories: M4PriceCategories) {
        MG4PriceCategory(category).openModal(block = true)
        getCategories(categories)
    }
}
