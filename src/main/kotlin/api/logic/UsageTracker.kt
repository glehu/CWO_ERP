package api.logic

import api.misc.json.UsageTrackerData
import api.misc.json.UsageTrackerStats
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
    runBlocking { readUsageStats() }
  }

  suspend fun writeUsageTrackingData(appCall: ApplicationCall) {
    val data =
      Timestamp.getUTCTimestampFromUnix(Timestamp.getUnixTimestamp()) + "<${Json.encodeToString(appCall.receive<UsageTrackerData>())}>\n"
    mutex.withLock { getUsageLogFile().appendText(data) }
    mutex.withLock { writeUsageStats() }
  }

  private suspend fun readUsageStats() {
    val usageStatsFile = getUsageStatsFile()
    val statsTxt = usageStatsFile.readText()
    if (statsTxt.isNotEmpty()) {
      val usageStats = Json.decodeFromString<UsageTrackerStats>(statsTxt)
      totalAPICalls.set(usageStats.totalAPICalls)
    }
  }

  private suspend fun writeUsageStats() {
    getUsageStatsFile().writeText(Json.encodeToString(UsageTrackerStats(totalAPICalls.incrementAndGet())))
  }

  private suspend fun getUsageLogFile(check: Boolean = true): File {
    if (check) checkFile(getUsageLogFile(false))
    return File(Paths.get(getPath(), "utrLog.txt").toString())
  }

  private suspend fun getUsageStatsFile(check: Boolean = true): File {
    if (check) checkFile(getUsageStatsFile(false))
    return File(Paths.get(getPath(), "utrStats.txt").toString())
  }

  private fun getPath(): String {
    return getModulePath("MX")
  }

  private suspend fun checkFile(fileToCheck: File): Boolean {
    val usageFilePath = File(getPath())
    if (!usageFilePath.isDirectory) {
      usageFilePath.mkdirs()
    }
    if (!fileToCheck.isFile) {
      withContext(Dispatchers.IO) {
        fileToCheck.createNewFile()
      }
      if (fileToCheck.isFile) {
        Log.log(Log.Type.SYS, "Usage Tracker file created.")
        return true
      }
    } else return true
    return false //Default no
  }
}
