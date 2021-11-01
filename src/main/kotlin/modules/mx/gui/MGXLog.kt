package modules.mx.gui

import api.misc.json.LogMsg
import javafx.collections.ObservableList
import tornadofx.*
import java.io.File
import java.io.InputStream

class MGXLog : Fragment("Log") {
    var logFile: File? = null

    /**
     * The filter criteria for log lookups.
     */
    private lateinit var regex: Regex
    private val logContent: ObservableList<LogMsg> = observableListOf()
    private val table = tableview(logContent) {
        readonlyColumn("ID", LogMsg::id)
        readonlyColumn("TStamp", LogMsg::tstamp)
        readonlyColumn("Type", LogMsg::type)
        readonlyColumn("Caller", LogMsg::caller)
        readonlyColumn("Text", LogMsg::msg).remainingWidth()
        readonlyColumn("User", LogMsg::user)
        readonlyColumn("Endpoint", LogMsg::apiEndpoint).minWidth(150.0)
        columnResizePolicy = SmartResize.POLICY
    }
    override val root = borderpane {
        minWidth = 1200.0
        minHeight = 800.0
        center = table
    }

    fun showLog(logFile: File, regex: Regex) {
        this.logFile = logFile
        logContent.clear()
        if (logFile.isFile) {
            this.regex = regex
            val inputStream: InputStream = logFile.inputStream()
            val tstampRgx = ".*<".toRegex()
            val typeRgx = "t:[^;]*;".toRegex()
            val callerRgx = "c:.*;".toRegex()
            val msgRgx = ":>.*".toRegex()
            val userRgx = "u:[^;]*;".toRegex()
            val apiEndpointRgx = "a:.*:".toRegex()
            var counter = 0
            inputStream.bufferedReader().forEachLine {
                if (it.uppercase().contains(regex)) {
                    counter++
                    logContent.add(
                        LogMsg(
                            id = counter,
                            tstamp = tstampRgx.find(it)?.value?.dropLast(1) ?: "",
                            type = typeRgx.find(it)?.value?.drop(2)?.dropLast(1) ?: "",
                            caller = callerRgx.find(it)?.value?.drop(2)?.dropLast(1) ?: "",
                            msg = msgRgx.find(it)?.value?.drop(2) ?: "",
                            user = userRgx.find(it)?.value?.drop(2)?.dropLast(1) ?: "",
                            apiEndpoint = apiEndpointRgx.find(it)?.value?.drop(2)?.dropLast(1) ?: ""
                        ),
                    )
                }
            }
        }
        openModal()
        table.refresh()
        table.smartResize()
    }
}
