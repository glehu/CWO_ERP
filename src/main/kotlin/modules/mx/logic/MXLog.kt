package modules.mx.logic

import modules.IModule
import modules.mx.getModulePath
import tornadofx.runAsync
import java.io.File
import java.time.LocalDateTime

class MXLog
{
    enum class LogType
    {
        INFO, WARNING, ERROR
    }
    companion object Logger: IModule
    {
        override fun moduleName() = "MXLog"

        private fun getLogPath(module: String) = "${getModulePath(module)}\\log"
        private fun getLogFile(module: String) = File("${getLogPath(module)}\\${module}_log.txt")

        fun log(module: String, type: LogType, text: String, caller: String, write: Boolean = true)
        {
            val logText = "<${type.toString().padEnd(8)}> ${caller.padEnd(20)} :> $text\n"
            print(logText)
            if (write)
            {
                runAsync {
                    getLogFile(module).appendText("${timestamp()}$logText")
                }
            }
        }

        fun timestamp(): LocalDateTime = LocalDateTime.now()

        fun checkLogFile(module: String, createIfMissing: Boolean, log: Boolean = true): Boolean
        {
            val logPath = File(getLogPath(module))
            if (!logPath.isDirectory)
            {
                if (createIfMissing)
                {
                    logPath.mkdirs()
                } else return false
            }
            val logFile = getLogFile(module)
            if (!logFile.isFile)
            {
                if (createIfMissing)
                {
                    logFile.createNewFile()
                    if (logFile.isFile)
                    {
                        if (log) log(module, LogType.INFO, "Log file created: $module", moduleName())
                        return true
                    }
                } else return false
            } else return true
            return false //Default no
        }

        fun deleteLogFile(module: String)
        {
            if (checkLogFile(module, false))
            {
                getLogFile(module).delete()
                log(module, LogType.INFO, "Log file cleared: $module", moduleName())
                checkLogFile(module, createIfMissing = true, log = false)
            }
        }

        fun deleteLogFiles()
        {
            deleteLogFile("MX")
            for (moduleNr in 1..99)
            {
                deleteLogFile("M$moduleNr")
            }
        }
    }
}