package api.logic

import api.misc.json.UsageTrackerData
import interfaces.IIndexManager
import interfaces.IModule
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.mx.logic.MXLog
import modules.mx.usageTracker
import tornadofx.Controller
import java.io.InputStream
import java.time.LocalDate

@ExperimentalSerializationApi
class MXAPIDashboard : IModule, Controller() {
    override val moduleNameLong = "MXAPIDashboard"
    override val module = "MX"
    override fun getIndexManager(): IIndexManager? {
        return null
    }

    fun getWebModuleUsageData(
        updateProgress: (Pair<Int, String>) -> Unit
    ): MutableMap<String, MutableMap<String, Int>> {
        var counter = 0
        val chartData = mutableMapOf<String, MutableMap<String, Int>>()
        var actionName: String
        var tstamp: String
        var trackerData: String
        lateinit var usageTrackerData: UsageTrackerData
        val tstampRgx = ".*<".toRegex()
        val tstampDayRgx = "-\\d\\dT".toRegex()
        val trackerDataRgx = "<.*>".toRegex()
        val inputStream: InputStream = usageTracker.getUsageLogFile().inputStream()
        log(MXLog.LogType.INFO, "Web module usage analysis start")
        inputStream.bufferedReader().forEachLine {
            updateProgress(Pair(++counter, "Mapping data..."))
            //Get Timestamp
            tstamp = tstampRgx.find(it)?.value?.dropLast(1) ?: ""
            val tstamptmp = tstampDayRgx.find(tstamp)?.value?.drop(1)?.dropLast(1) ?: "?"
            //Get Tracking Data
            trackerData = trackerDataRgx.find(it)?.value?.drop(1)?.dropLast(1) ?: ""
            //Put Tracking Data in the map
            if (trackerData.isNotEmpty()) {
                usageTrackerData = Json.decodeFromString(trackerData)
                actionName = usageTrackerData.action
                if (chartData.containsKey(actionName)) {
                    if (chartData[actionName]!!.containsKey(tstamptmp)) {
                        chartData[actionName]!![tstamptmp] = chartData[actionName]!![tstamptmp]!! + 1
                    }
                } else {
                    val mmap = mutableMapOf<String, Int>()
                    for (daysMinus in 7L downTo 0L) {
                        mmap[LocalDate.now().minusDays(daysMinus).dayOfMonth
                            .toString().padStart(2, '0')] = 0
                    }
                    if (mmap.containsKey(tstamptmp)) {
                        mmap[tstamptmp] = 1
                    }
                    chartData[actionName] = mmap
                }
            }
        }
        return chartData
    }
}
