package modules.mx.logic

import interfaces.IModule
import modules.mx.activeUser
import modules.mx.getModulePath
import modules.mx.logic.MXTimestamp.MXTimestamp.getUTCTimestampFromUnix
import modules.mx.logic.MXTimestamp.MXTimestamp.getUnixTimestamp
import tornadofx.runAsync
import java.io.File

class MXLog {
    enum class LogType {
        INFO, WARNING, ERROR, COM
    }

    companion object MXLog : IModule {
        override fun moduleNameLong() = "MXLog"
        override fun module() = "MX"

        private fun getLogPath(module: String) = "${getModulePath(module)}\\log"
        private fun getLogFile(module: String) = File("${getLogPath(module)}\\${module}_log.txt")

        fun log(module: String, type: LogType, text: String, caller: String, write: Boolean = true) {
            val logText = "<$type><${activeUser.username}> $caller :> $text\n"
            print(logText)
            if (write) {
                runAsync {
                    getLogFile(module).appendText("${getUTCTimestampFromUnix(getUnixTimestamp())}$logText")
                }
            }
        }

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
                        if (log) log(module, LogType.INFO, "Log file created: $module", moduleNameLong())
                        return true
                    }
                } else return false
            } else return true
            return false //Default no
        }

        fun deleteLogFile(module: String) {
            if (checkLogFile(module, false)) {
                getLogFile(module).delete()
                log(module, LogType.INFO, "Log file cleared: $module", moduleNameLong())
                checkLogFile(module, createIfMissing = true, log = false)
            }
        }

        fun deleteLogFiles() {
            deleteLogFile("MX")
            for (moduleNr in 1..99) {
                deleteLogFile("M$moduleNr")
            }
        }
    }
}