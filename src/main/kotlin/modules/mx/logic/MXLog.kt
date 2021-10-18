package modules.mx.logic

import interfaces.IIndexManager
import interfaces.IModule
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.activeUser
import modules.mx.getModulePath
import modules.mx.isClientGlobal
import modules.mx.logic.MXTimestamp.MXTimestamp.getUTCTimestampFromUnix
import modules.mx.logic.MXTimestamp.MXTimestamp.getUnixTimestamp
import java.io.File
import kotlin.concurrent.thread

class MXLog {
    enum class LogType {
        INFO, WARNING, ERROR, COM
    }

    @ExperimentalSerializationApi
    companion object MXLog : IModule {
        override val moduleNameLong = "MXLog"
        override val module = "MX"
        override fun getIndexManager(): IIndexManager? {
            return null
        }

        private fun getLogPath(module: String) = "${getModulePath(module)}\\log"
        private fun getLogFile(module: String) = File("${getLogPath(module)}\\${module}_log.txt")

        /**
         * Writes a log message to the disk.
         */
        fun log(module: String, type: LogType, text: String, caller: String, write: Boolean = true) {
            if (!isClientGlobal) {
                val logText = "<$type><${activeUser.username}> $caller :> $text\n"
                print(logText)
                if (write) {
                    thread {
                        getLogFile(module).appendText("${getUTCTimestampFromUnix(getUnixTimestamp())}$logText")
                    }
                }
            }
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
                        if (log) log(LogType.INFO, "Log file created: $module")
                        return true
                    }
                } else return false
            } else return true
            return false //Default no
        }

        /**
         * Deletes a single log file of a provided module.
         */
        fun deleteLogFile(module: String) {
            if (checkLogFile(module, false)) {
                getLogFile(module).delete()
                log(LogType.INFO, "Log file cleared: $module")
                checkLogFile(module, createIfMissing = true, log = false)
            }
        }

        /**
         * Deletes all log files across all modules.
         */
        fun deleteLogFiles() {
            deleteLogFile("MX")
            for (moduleNr in 1..99) {
                deleteLogFile("M$moduleNr")
            }
        }
    }
}
