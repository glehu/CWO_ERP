package modules.m4.logic

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
import modules.m4.M4Storage
import modules.m4.M4StorageUnit
import modules.m4.M4Storages
import modules.m4.gui.MG4Storage
import modules.m4.gui.MG4StorageManager
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
class M4StorageManager : IModule, Controller() {
    override val moduleNameLong = "M4StorageManager"
    override val module = "M4"
    override fun getIndexManager(): IIndexManager? {
        return null
    }

    fun funUpdateStorage(storageNew: M4Storage, storageOld: M4Storage) {
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
        if (!cliMode) find<MG4StorageManager>().refreshStorages()
    }

    fun deleteStorage(storage: M4Storage) {
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
        if (!cliMode) tornadofx.find<MG4StorageManager>().refreshStorages()
    }

    fun getStorages(): M4Storages {
        lateinit var storages: M4Storages
        if (!isClientGlobal) {
            val storageFile = getStoragesFile()
            if (!storageFile.isFile) initializeCategories(storageFile)
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

    private fun writeStorages(categories: M4Storages) {
        getStoragesFile().writeText(Json.encodeToString(categories))
    }

    private fun getStoragesFile() = File("${getModulePath(module)}\\storages.dat")

    private fun initializeCategories(storageFile: File) {
        val mainStorage = M4Storage(0, "default")
        mainStorage.storageUnits.add(M4StorageUnit(0, ""))
        val categories = M4Storages(CategoryType.MAIN)
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
    fun getNumber(categories: M4Storages): Int {
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

    fun addStorage(categories: M4Storages) =
        showCategory(M4Storage(getNumber(categories), ""), categories)

    fun getStorages(categories: M4Storages): ObservableList<M4Storage> {
        val priceCategories = observableListOf<M4Storage>()
        val sortedMap = categories.storages.toSortedMap()
        for ((_, v) in sortedMap) priceCategories.add(v)
        return priceCategories
    }

    fun showCategory(category: M4Storage, categories: M4Storages) {
        MG4Storage(category).openModal(block = true)
        getStorages(categories)
    }
}
