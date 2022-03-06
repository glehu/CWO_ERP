package api.logic

import api.misc.json.UsageTrackerData
import api.misc.json.UsageTrackerStats
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.util.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.getModulePath
import modules.mx.logic.Log
import modules.mx.logic.Timestamp
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicLong

@InternalAPI
@ExperimentalSerializationApi
class UsageTracker {
  var totalAPICalls: AtomicLong = AtomicLong(0)
  private val mutex = Mutex()

  init {
    readUsageStats()
  }

  suspend fun writeUsageTrackingData(appCall: ApplicationCall) {
    val data = Timestamp.getUTCTimestampFromUnix(Timestamp.getUnixTimestamp()) +
            "<${Json.encodeToString(appCall.receive<UsageTrackerData>())}>\n"
    mutex.withLock { getUsageLogFile().appendText(data) }
    mutex.withLock { writeUsageStats() }
  }

  private fun readUsageStats() {
    val usageStatsFile = getUsageStatsFile()
    val statsTxt = usageStatsFile.readText()
    if (statsTxt.isNotEmpty()) {
      val usageStats = Json.decodeFromString<UsageTrackerStats>(statsTxt)
      totalAPICalls.set(usageStats.totalAPICalls)
    }
  }

  private fun writeUsageStats() {
    getUsageStatsFile().writeText(Json.encodeToString(UsageTrackerStats(totalAPICalls.incrementAndGet())))
  }

  fun getUsageLogFile(check: Boolean = true): File {
    if (check) checkFile(getUsageLogFile(false))
    return File(Paths.get(getPath(), "utrLog.txt").toString())
  }

  private fun getUsageStatsFile(check: Boolean = true): File {
    if (check) checkFile(getUsageStatsFile(false))
    return File(Paths.get(getPath(), "utrStats.txt").toString())
  }

  private fun getPath(): String {
    return getModulePath("MX")
  }

  private fun checkFile(fileToCheck: File): Boolean {
    val usageFilePath = File(getPath())
    if (!usageFilePath.isDirectory) {
      usageFilePath.mkdirs()
    }
    if (!fileToCheck.isFile) {
      fileToCheck.createNewFile()
      if (fileToCheck.isFile) {
        Log.log(Log.LogType.SYS, "Usage Tracker file created.")
        return true
      }
    } else return true
    return false //Default no
  }
}
