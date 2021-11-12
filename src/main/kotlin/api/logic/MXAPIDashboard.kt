package api.logic

import api.misc.json.UsageTrackerData
import interfaces.IIndexManager
import interfaces.IModule
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.mx.logic.MXLog
import modules.mx.logic.MXTimestamp.MXTimestamp.getLocalHour
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

    private val tstampRgx = ".*<".toRegex()
    private val trackerDataRgx = "<.*>".toRegex()
    private val tstampDateRgx = ".*T".toRegex()
    private val tstampTimeRgx = "T.*Z".toRegex()
    private val tstampMonthDayRgx = "\\d\\d-\\d\\dT".toRegex()
    private var counter = 0
    private val chartData = mutableMapOf<String, MutableMap<String, Int>>()
    private lateinit var actionName: String
    private lateinit var tstamp: String
    private lateinit var trackerData: String
    private lateinit var usageTrackerData: UsageTrackerData

    fun getWebUsageDays(
        amountOfDays: Long,
        updateProgress: (Pair<Int, String>) -> Unit
    ): MutableMap<String, MutableMap<String, Int>> {
        counter = 0
        val inputStream = getInputStream()
        log(MXLog.LogType.INFO, "Web module usage analysis start")
        inputStream.bufferedReader().forEachLine {
            updateProgress(Pair(++counter, "Mapping data..."))
            //Get Timestamp
            tstamp = tstampRgx.find(it)?.value?.dropLast(1) ?: ""
            val tstamptmp = tstampMonthDayRgx.find(tstamp)?.value?.dropLast(1) ?: "?"
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
                    for (daysMinus in (amountOfDays - 1) downTo 0L) {
                        val datTmp = LocalDate.now().minusDays(daysMinus)
                        mmap[
                                "${datTmp.monthValue.toString().padStart(2, '0')}-" +
                                        datTmp.dayOfMonth.toString().padStart(2, '0')
                        ] = 0
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

    private fun getInputStream(): InputStream {
        return usageTracker.getUsageLogFile().inputStream()
    }

    fun getWebUsageToday(
        updateProgress: (Pair<Int, String>) -> Unit
    ): MutableMap<String, MutableMap<String, Int>> {
        counter = 0
        val inputStream = getInputStream()
        val datTmp = LocalDate.now()
        val today = "${datTmp.monthValue.toString().padStart(2, '0')}-" +
                datTmp.dayOfMonth.toString().padStart(2, '0')
        log(MXLog.LogType.INFO, "Web module usage analysis start")
        inputStream.bufferedReader().forEachLine {
            updateProgress(Pair(++counter, "Mapping data..."))
            //Get Timestamp
            tstamp = tstampRgx.find(it)?.value?.dropLast(1) ?: ""
            val date = tstampDateRgx.find(tstamp)?.value?.drop(5)?.dropLast(1) ?: "?"
            if (date != "?" && date == today) {
                val hour = getLocalHour(
                    (tstampTimeRgx.find(tstamp)?.value?.drop(1)?.dropLast(7) ?: "?")
                ) + ":00"
                //Get Tracking Data
                trackerData = trackerDataRgx.find(it)?.value?.drop(1)?.dropLast(1) ?: ""
                //Put Tracking Data in the map
                if (trackerData.isNotEmpty()) {
                    usageTrackerData = Json.decodeFromString(trackerData)
                    actionName = usageTrackerData.action
                    if (chartData.containsKey(actionName)) {
                        if (chartData[actionName]!!.containsKey(hour)) {
                            chartData[actionName]!![hour] = chartData[actionName]!![hour]!! + 1
                        }
                    } else {
                        val mmap = mutableMapOf<String, Int>()
                        for (hourCounter in 0..24) {
                            mmap["${hourCounter.toString().padStart(2, '0')}:00"] = 0
                        }
                        if (mmap.containsKey(date)) {
                            mmap[date] = 1
                        }
                        chartData[actionName] = mmap
                    }
                }
            }
        }
        return chartData
    }
}
