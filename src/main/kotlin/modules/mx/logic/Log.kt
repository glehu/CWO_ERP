package modules.mx.logic

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.LogMessage
import modules.mx.getModulePath
import java.io.File
import java.nio.file.Paths

class Log {
  enum class Type {
    INFO, WARNING, ERROR, COM, SYS
  }

  @InternalAPI
  @ExperimentalSerializationApi
  companion object MXLog : IModule {
    override val moduleNameLong = "Log"
    override val module = "MX"
    override fun getIndexManager(): IIndexManager? {
      return null
    }

    private val mutex = Mutex()

    private fun getLogPath(module: String) = Paths.get(getModulePath(module), "log").toString()
    private fun getLogFile(module: String) = File(Paths.get(getLogPath(module), "${module}_log.txt").toString())

    /**
     * Writes a log message to the disk.
     */
    suspend fun log(
      module: String,
      type: Type,
      text: String,
      caller: String,
      write: Boolean = true,
      apiEndpoint: String = ""
    ) {
      val logMessageSerialized = Json.encodeToString(
        LogMessage(
          Timestamp.getUnixTimestampHex(),
          module,
          type,
          text,
          caller,
          apiEndpoint
        )
      ) + "\n"
      print(logMessageSerialized)
      if (write) {
        mutex.withLock {
          getLogFile(module).appendText(logMessageSerialized)
        }
      }
    }

    /**
     * Checks a log file and creates it if it's missing and createIfMissing is set to true.
     */
    suspend fun checkLogFile(module: String, createIfMissing: Boolean, log: Boolean = true): Boolean {
      val logPath = File(getLogPath(module))
      if (!logPath.isDirectory) {
        if (createIfMissing) {
          logPath.mkdirs()
        } else return false
      }
      val logFile = getLogFile(module)
      if (!logFile.isFile) {
        if (createIfMissing) {
          withContext(Dispatchers.IO) {
            logFile.createNewFile()
          }
          if (logFile.isFile) {
            if (log) log(Type.INFO, "Log file created: $module")
            return true
          }
        } else return false
      } else return true
      return false //Default no
    }
  }
}
