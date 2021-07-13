package modules.m2.logic

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import modules.IModule
import modules.m2.Contact
import modules.mx.MXLog
import tornadofx.Controller
import kotlin.system.measureTimeMillis

class M2Analytics : IModule, Controller()
{
    override fun moduleName() = "M2Analytics"
    val db: CwODB by inject()

    @ExperimentalSerializationApi
    fun getChartDataOnCityDistribution(
        indexManager: M2IndexManager,
        updateProgress: (Pair<Int, String>) -> Unit
    ): MutableMap<String, Double>
    {
        var contactCount = 0.0
        val map = mutableMapOf<String, Double>()
        val dbManager = M2DBManager()
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
                    contactCount += 1.0
                    if (map.containsKey(contact.city))
                    {
                        map[contact.city] = map[contact.city]!! + 1.0
                    } else map[contact.city] = 1.0
                }
            }
            map["[amount]"] = contactCount
        }
        MXLog.log("M2", MXLog.LogType.INFO, "City distribution analysis end (${timeInMS / 1000} sec)", moduleName())
        return map.toSortedMap()
    }
}