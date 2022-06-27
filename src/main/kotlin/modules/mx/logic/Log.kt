package modules.mx.logic

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
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

    private fun getLogPath(module: String) = Paths.get(getModulePath(module), "log").toString()
    private fun getLogFile(module: String) = File(Paths.get(getLogPath(module), "${module}_log.txt").toString())

    /**
     * Writes a log message to the disk.
     */
    fun log(
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
      if (write) getLogFile(module).appendText(logMessageSerialized)
    }

    /**
     * Checks a log file and creates it if it's missing and createIfMissing is set to true.
     */
    fun checkLogFile(module: String, createIfMissing: Boolean, log: Boolean = true): Boolean {
      val logPath = File(getLogPath(module))
      if (!logPath.isDirectory) {
        if (createIfMissing) {
          logPath.mkdirs()
        } else return false
      }
      val logFile = getLogFile(module)
      if (!logFile.isFile) {
        if (createIfMissing) {
          logFile.createNewFile()
          if (logFile.isFile) {
            if (log) log(Type.INFO, "Log file created: $module")
            return true
          }
        } else return false
      } else return true
      return false //Default no
    }

    /**
     * Deletes a single log file of a provided module.
     */
    private fun deleteLogFile(module: String) {
      if (checkLogFile(module, false)) {
        getLogFile(module).delete()
        log(Type.INFO, "Log file cleared: $module")
        checkLogFile(module, createIfMissing = true, log = false)
      }
    }

    /**
     * Deletes all log files across all modules.
     */
    fun deleteLogFiles() {
      for (moduleNr in 1..99) {
        deleteLogFile("M$moduleNr")
      }
      deleteLogFile("M4SP")
      deleteLogFile("MX")
    }
  }
}
