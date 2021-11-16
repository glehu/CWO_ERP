package modules.m1.logic

import db.CwODB
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.M1Song
import modules.mx.logic.MXLog
import modules.mx.m1GlobalIndex
import tornadofx.Controller
import kotlin.system.measureTimeMillis

@InternalAPI
@ExperimentalSerializationApi
class M1Analytics : IModule, Controller() {
    override val moduleNameLong = "M1Analytics"
    override val module = "M1"
    override fun getIndexManager(): IIndexManager {
        return m1GlobalIndex!!
    }

    enum class DistType {
        GENRE, TYPE
    }

    @ExperimentalSerializationApi
    fun getDistributionChartData(
        distType: DistType,
        updateProgress: (Pair<Int, String>) -> Unit
    ): MutableMap<String, Double> {
        var songCount = 0.0
        val map = mutableMapOf<String, Double>()
        var distTypeData: String
        log(MXLog.LogType.INFO, "Distribution analysis start")
        val timeInMS = measureTimeMillis {
            CwODB.getEntriesFromSearchString(
                searchText = "",
                ixNr = 0,
                exactSearch = false,
                maxSearchResults = -1,
                indexManager = m1GlobalIndex!!
            )
            { uID, entryBytes ->
                updateProgress(Pair(uID, "Mapping data..."))
                val song = decode(entryBytes) as M1Song
                if (song.uID != -1) {
                    songCount++
                    distTypeData = when (distType) {
                        DistType.GENRE -> song.genre
                        DistType.TYPE -> song.type
                    }
                    if (map.containsKey(distTypeData)) {
                        map[distTypeData] = map[distTypeData]!! + 1.0
                    } else {
                        map[distTypeData] = 1.0
                    }
                }
            }
            map["[amount]"] = songCount
        }
        log(MXLog.LogType.INFO, "Distribution analysis end (${timeInMS / 1000} sec)")
        return map.toSortedMap()
    }
}
