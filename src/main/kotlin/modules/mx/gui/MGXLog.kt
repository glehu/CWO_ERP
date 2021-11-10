package modules.mx.gui

import api.misc.json.LogMsg
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.geometry.Orientation
import javafx.scene.control.TextField
import modules.mx.gui.userAlerts.MGXUserAlert
import modules.mx.rightButtonsWidth
import tornadofx.*
import java.io.File
import java.io.InputStream
import kotlin.math.roundToInt

class MGXLog(title: String) : Fragment(title) {
    private var logFile: File? = null

    /**
     * The filter criteria for log lookups.
     */
    private lateinit var regex: Regex
    private val logContent: ObservableList<LogMsg> = observableListOf()
    private val logDisplay: ObservableList<LogMsg> = observableListOf()
    private val results = SimpleIntegerProperty(0)
    private val maxResults = SimpleIntegerProperty(0)
    private val resultsText = SimpleStringProperty("")
    private var searchText: TextField by singleAssign()
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
                    separator(Orientation.VERTICAL).paddingHorizontal = 25
                    vbox {
                        button("Filter (Enter)") {
                            shortcut("Enter")
                            prefWidth = rightButtonsWidth
                            action {
                                filterLog(searchText.text)
                            }
                        }
                        button("Reset (CTRL-X)") {
                            shortcut("CTRL+X")
                            prefWidth = rightButtonsWidth
                            action {
                                searchText.text = ".*"
                                filterLog(".*")
                                searchText.selectAll()
                            }
                        }
                    }
                    button("Reload (CTRL+R)") {
                        shortcut("CTRL+R")
                        prefWidth = rightButtonsWidth
                        action {
                            showLog(logFile!!, ".*".toRegex())
                        }
                    }
                    separator(Orientation.VERTICAL).paddingHorizontal = 25
                    label(resultsText)
                }
            }
        }
        center = table
    }

    fun showLog(logFile: File, regex: Regex) {
        val loadingNotification = MGXUserAlert("Loading...")
        loadingNotification.openModal()
        this.logFile = logFile
        logContent.clear()
        logDisplay.clear()
        results.value = 0
        searchText.text = ".*"
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
                    results += 1
                }
            }
            if (maxResults.value == 0) {
                maxResults.value = results.value
            }
            resultsText.value = results.value.toString() + " / " + maxResults.value.toString()
        }
        loadingNotification.close()
        openModal()
        table.refresh()
        table.smartResize()
        searchText.requestFocus()
        searchText.selectAll()
    }

    private fun filterLog(filter: String) {
        val regex: Regex = if (filter.isNotEmpty()) {
            filter.uppercase().toRegex()
        } else ".*".toRegex()
        logDisplay.clear()
        results.value = 0
        for (msg in logContent) {
            if (msg.toString().uppercase().contains(regex)) {
                logDisplay.add(msg)
                results += 1
            }
        }
        resultsText.value =
            "${results.value} / ${maxResults.value} (${((results.value.toFloat() / maxResults.value.toFloat()) * 100).roundToInt()}%)"
        table.refresh()
        table.smartResize()
    }
}
