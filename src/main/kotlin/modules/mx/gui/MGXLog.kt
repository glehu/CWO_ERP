package modules.mx.gui

import api.misc.json.LogMsg
import javafx.collections.ObservableList
import javafx.scene.control.TextField
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
    private val logDisplay: ObservableList<LogMsg> = observableListOf()
    var searchText: TextField by singleAssign()
    private val table = tableview(logDisplay) {
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
        top = form {
            fieldset {
                hbox {
                    field("Search") {
                        searchText = textfield {
                            tooltip("Contains the search text that will be used to find an entry.")
                        }
                    }
                    button("Filter (Enter)") {
                        shortcut("Enter")
                        action {
                            filterLog(searchText.text)
                        }
                    }
                    button("Reload (CTRL+R)") {
                        shortcut("CTRL+R")
                        action {
                            showLog(logFile!!, ".*".toRegex())
                        }
                    }
                }
            }
        }
        center = table
    }

    fun showLog(logFile: File, regex: Regex) {
        this.logFile = logFile
        logContent.clear()
        logDisplay.clear()
        searchText.text = ""
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
                    val msgT = LogMsg(
                        id = counter,
                        tstamp = tstampRgx.find(it)?.value?.dropLast(1) ?: "",
                        type = typeRgx.find(it)?.value?.drop(2)?.dropLast(1) ?: "",
                        caller = callerRgx.find(it)?.value?.drop(2)?.dropLast(1) ?: "",
                        msg = msgRgx.find(it)?.value?.drop(2) ?: "",
                        user = userRgx.find(it)?.value?.drop(2)?.dropLast(1) ?: "",
                        apiEndpoint = apiEndpointRgx.find(it)?.value?.drop(2)?.dropLast(1) ?: ""
                    )
                    logContent.add(msgT)
                    logDisplay.add(msgT)
                }
            }
        }
        openModal()
        table.refresh()
        table.smartResize()
    }

    private fun filterLog(filter: String) {
        val regex: Regex = if (filter.isNotEmpty()) {
            filter.uppercase().toRegex()
        } else ".*".toRegex()
        logDisplay.clear()
        for (msg in logContent) {
            if (msg.toString().uppercase().contains(regex)) {
                logDisplay.add(msg)
            }
        }
        table.refresh()
        table.smartResize()
    }
}
