package modules.m1.logic

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import modules.interfaces.IModule
import modules.m1.Song
import modules.mx.logic.MXLog
import tornadofx.Controller
import kotlin.system.measureTimeMillis

class M1Analytics : IModule, Controller()
{
    override fun moduleNameLong() = "M1Analytics"
    override fun module() = "M1"
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
        MXLog.log(module(), MXLog.LogType.INFO, "Genre distribution analysis start", moduleNameLong())
        val timeInMS = measureTimeMillis {
            db.getEntriesFromSearchString(
                "", 0, false, module(), -1, indexManager
            )
            { uID, entryBytes ->
                updateProgress(Pair(uID, "Mapping genre data..."))
                val song: Song = dbManager.decodeEntry(entryBytes) as Song
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
        MXLog.log(module(), MXLog.LogType.INFO, "Genre distribution analysis end (${timeInMS / 1000} sec)", moduleNameLong())
        return map.toSortedMap()
    }
}