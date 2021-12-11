package modules.m2.logic

import db.CwODB
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.Contact
import modules.mx.logic.Log
import modules.mx.m2GlobalIndex
import tornadofx.Controller
import kotlin.system.measureTimeMillis

@InternalAPI
@ExperimentalSerializationApi
class ContactAnalytics : IModule, Controller() {
    override val moduleNameLong = "M2Analytics"
    override val module = "M2"
    override fun getIndexManager(): IIndexManager {
        return m2GlobalIndex!!
    }

    fun getChartDataOnCityDistribution(
        indexManager: ContactIndexManager,
        amount: Int = -1,
        updateProgress: (Pair<Int, String>) -> Unit
    ): MutableMap<String, Double> {
        val tempMap = mutableMapOf<String, Double>()
        lateinit var sortedMap: MutableMap<String, Double>
        var map = mutableMapOf<String, Double>()
        var contactCount = 0.0
        var amountCount = 0
        var city: String
        log(Log.LogType.INFO, "City distribution analysis start")
        val timeInMS = measureTimeMillis {
            CwODB.getEntriesFromSearchString(
                searchText = "",
                ixNr = 0,
                exactSearch = false,
                maxSearchResults = -1,
                indexManager = indexManager
            )                       // shout out blkghst
            { uID, entryBytes ->
                updateProgress(Pair(uID, "Mapping city data..."))
                val contact: Contact = decode(entryBytes) as Contact
                if (contact.uID != -1) {
                    city = contact.city.uppercase()
                    contactCount += 1.0
                    if (tempMap.containsKey(city)) {
                        tempMap[city] = tempMap[city]!! + 1.0
                    } else tempMap[city] = 1.0
                }
            }
            if (amount != -1) {
                sortedMap = mutableMapOf()
                tempMap.entries.sortedBy { it.value }.reversed().forEach { sortedMap[it.key] = it.value }
                for ((k, v) in sortedMap) {
                    if (amountCount < amount) {
                        map[k] = v
                    } else {
                        //Here we sum the remaining entries
                        if (map.containsKey("...")) {
                            map["..."] = map["..."]!! + v
                        } else map["..."] = v
                    }
                    amountCount++
                }
            } else {
                map = tempMap
            }
            map["[amount]"] = contactCount
        }
        log(Log.LogType.INFO, "City distribution analysis end (${timeInMS / 1000} sec)")
        return map.toSortedMap()
    }
}
