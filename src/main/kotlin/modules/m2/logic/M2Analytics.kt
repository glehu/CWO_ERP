package modules.m2.logic

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import modules.IModule
import modules.m2.Contact
import modules.mx.logic.MXLog
import tornadofx.Controller
import kotlin.system.measureTimeMillis

class M2Analytics : IModule, Controller()
{
    override fun moduleName() = "M2Analytics"
    val db: CwODB by inject()

    @ExperimentalSerializationApi
    fun getChartDataOnCityDistribution(
        indexManager: M2IndexManager,
        amount: Int = -1,
        updateProgress: (Pair<Int, String>) -> Unit
    ): MutableMap<String, Double>
    {
        val dbManager = M2DBManager()
        val tempMap = mutableMapOf<String, Double>()
        lateinit var sortedMap: MutableMap<String, Double>
        var map = mutableMapOf<String, Double>()
        var contactCount = 0.0
        var amountCount = 0
        var city: String
        MXLog.log("M2", MXLog.LogType.INFO, "City distribution analysis start", moduleName())
        val timeInMS = measureTimeMillis {
            db.getEntriesFromSearchString(
                "", 0, false, "M2", -1, indexManager
            )
            { uID, entryBytes ->
                updateProgress(Pair(uID, "Mapping city data..."))
                val contact: Contact = dbManager.getEntry(entryBytes) as Contact
                if (contact.uID != -1)
                {
                    city = contact.city.uppercase()
                    contactCount += 1.0
                    if (tempMap.containsKey(city))
                    {
                        tempMap[city] = tempMap[city]!! + 1.0
                    } else tempMap[city] = 1.0
                }
            }
            if (amount != -1)
            {
                sortedMap = mutableMapOf()
                tempMap.entries.sortedBy { it.value }.reversed().forEach { sortedMap[it.key] = it.value }
                for ((k, v) in sortedMap)
                {
                    if (amountCount < amount)
                    {
                        map[k] = v
                    } else
                    {
                        //Here we sum the remaining entries
                        if (map.containsKey("..."))
                        {
                            map["..."] = map["..."]!! + v
                        } else map["..."] = v
                    }
                    amountCount++
                }
            } else
            {
                map = tempMap
            }
            map["[amount]"] = contactCount
        }
        MXLog.log(
            "M2", MXLog.LogType.INFO, "City distribution analysis end (${timeInMS / 1000} sec)",
            moduleName()
        )
        return map.toSortedMap()
    }
}