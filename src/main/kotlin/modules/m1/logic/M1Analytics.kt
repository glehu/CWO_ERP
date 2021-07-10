package modules.m1.logic

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import modules.Module
import modules.m1.Song
import modules.mx.MXLog
import tornadofx.Controller
import kotlin.system.measureTimeMillis

class M1Analytics : Module, Controller()
{
    override fun moduleName() = "M1Analytics"
    val db: CwODB by inject()

    @ExperimentalSerializationApi
    fun getChartDataOnGenreDistribution(
        indexManager: M1IndexManager,
        updateProgress: (Pair<Int, String>) -> Unit
    ): MutableMap<String, Double>
    {
        var songCount = 0.0
        val map = mutableMapOf<String, Double>()
        val dbManager = M1DBManager()
        MXLog.log("M1", MXLog.LogType.INFO, "Genre distribution analysis start ${MXLog.timestamp()}", moduleName())
        val timeInMS = measureTimeMillis {
            db.getEntriesFromSearchString(
                "", 0, false, "M1", -1, indexManager
            )
            { uID, entryBytes ->
                updateProgress(Pair(uID, "Mapping genre data..."))
                val song: Song = dbManager.getEntry(entryBytes) as Song
                if (song.uID != -1)
                {
                    songCount += 1.0
                    if (map.containsKey(song.genre))
                    {
                        map[song.genre] = map[song.genre]!! + 1.0
                    } else
                    {
                        map[song.genre] = 1.0
                    }
                }
            }
            map["[amount]"] = songCount
        }
        MXLog.log("M1", MXLog.LogType.INFO, "Genre distribution analysis end (${timeInMS / 1000} sec)", moduleName())
        return map.toSortedMap()
    }
}