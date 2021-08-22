package modules.m1.logic

import db.CwODB
import interfaces.IModule
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.Song
import modules.mx.logic.MXLog
import tornadofx.Controller
import kotlin.system.measureTimeMillis

class M1Analytics : IModule, Controller()
{
    override fun moduleNameLong() = "M1Analytics"
    override fun module() = "M1"
    val db: CwODB by inject()

    enum class DistType
    {
        GENRE, TYPE
    }

    @ExperimentalSerializationApi
    fun getDistributionChartData(
        indexManager: M1IndexManager,
        distType: DistType,
        updateProgress: (Pair<Int, String>) -> Unit
    ): MutableMap<String, Double>
    {
        var songCount = 0.0
        val map = mutableMapOf<String, Double>()
        val dbManager = M1DBManager()
        var distTypeData: String
        MXLog.log(module(), MXLog.LogType.INFO, "Distribution analysis start", moduleNameLong())
        val timeInMS = measureTimeMillis {
            db.getEntriesFromSearchString(
                "", 0, false, module(), -1, indexManager
            )
            { uID, entryBytes ->
                updateProgress(Pair(uID, "Mapping data..."))
                val song: Song = dbManager.decodeEntry(entryBytes) as Song
                if (song.uID != -1)
                {
                    songCount += 1.0
                    distTypeData = when(distType)
                    {
                        DistType.GENRE -> song.genre
                        DistType.TYPE -> song.type
                    }
                    if (map.containsKey(distTypeData))
                    {
                        map[distTypeData] = map[distTypeData]!! + 1.0
                    } else
                    {
                        map[distTypeData] = 1.0
                    }
                }
            }
            map["[amount]"] = songCount
        }
        MXLog.log(
            module(),
            MXLog.LogType.INFO, "Distribution analysis end (${timeInMS / 1000} sec)", moduleNameLong()
        )
        return map.toSortedMap()
    }
}