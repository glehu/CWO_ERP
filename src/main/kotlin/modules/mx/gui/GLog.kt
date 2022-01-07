package modules.mx.gui

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.geometry.Orientation
import javafx.scene.control.TextField
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.mx.LogMessage
import modules.mx.gui.userAlerts.GAlert
import modules.mx.logic.Timestamp
import modules.mx.rightButtonsWidth
import tornadofx.*
import java.io.File
import java.io.InputStream
import kotlin.math.roundToInt
import kotlin.reflect.full.memberProperties

class GLog(title: String) : Fragment(title) {
    private var logFile: File? = null

    /**
     * The filter criteria for log lookups.
     */
    private lateinit var regex: Regex
    private val logContent: ObservableList<LogMessage> = observableListOf()
    private val logDisplay: ObservableList<LogMessage> = observableListOf()
    private val results = SimpleIntegerProperty(0)
    private val maxResults = SimpleIntegerProperty(0)
    private val resultsText = SimpleStringProperty("")
    private var searchText: TextField by singleAssign()
    private val table = tableview(logDisplay) {
        readonlyColumn("TStamp", LogMessage::unixTimestamp)
            .cellFormat {
                text = Timestamp.getLocalTimestamp(Timestamp.convUnixHexToUnixTimestamp(rowItem.unixTimestamp))
            }
        readonlyColumn("Module", LogMessage::module)
        readonlyColumn("Type", LogMessage::type)
        readonlyColumn("Text", LogMessage::text).remainingWidth()
        readonlyColumn("Caller", LogMessage::caller)
        readonlyColumn("Endpoint", LogMessage::apiEndpoint).minWidth(150.0)
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
                            tooltip("Contains the regex pattern the log file will be filtered with.")
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
                            tooltip("Filters the log file with the provided regex pattern.")
                        }
                        button("Reset (CTRL-X)") {
                            shortcut("CTRL+X")
                            prefWidth = rightButtonsWidth
                            action {
                                searchText.text = ".*"
                                filterLog(".*")
                                searchText.selectAll()
                            }
                            tooltip("Resets the filter.")
                        }
                    }
                    vbox {
                        button("Reload (CTRL+R)") {
                            shortcut("CTRL+R")
                            prefWidth = rightButtonsWidth
                            action {
                                showLog(logFile!!, ".*".toRegex())
                            }
                            tooltip("Reloads the log file from the disk.")
                        }
                        button("Save (CTRL+S)") {
                            shortcut("CTRL+S")
                            prefWidth = rightButtonsWidth
                            action {
                                saveLog()
                            }
                            tooltip("Saves the filtered log file as a CSV file.")
                        }
                    }
                    separator(Orientation.VERTICAL).paddingHorizontal = 25
                    label(resultsText)
                }
            }
        }
        center = table
    }

    private fun saveLog() {
        val directory = chooseDirectory("Choose directory for the CSV file to be saved in:")
        val timestamp = Timestamp.getUTCTimestamp(Timestamp.getUnixTimestamp()).replace(':', '-')
        val file = File(
            directory!!.path + "\\Log_$title$-$timestamp.csv"
        )
        file.createNewFile()
        val list = logDisplay.toList()
        csvWriter { delimiter = ';' }.open(file) {
            val header = arrayListOf<String>()
            val map = list[0].asMap()
            for (property in map) {
                header.add(property.key)
            }
            writeRow(header)
            for (i in list.indices) {
                val data = arrayListOf<String>()
                val mapp = list[i].asMap()
                for (value in mapp.values) {
                    data.add(value.toString())
                }
                writeRow(data)
            }
        }
    }

    private inline fun <reified T : Any> T.asMap(): Map<String, Any?> {
        val props = T::class.memberProperties.associateBy { it.name }
        return props.keys.associateWith { props[it]?.get(this) }
    }

    fun showLog(logFile: File, regex: Regex) {
        val loadingNotification = GAlert("Loading...")
        loadingNotification.openModal()
        this.logFile = logFile
        logContent.clear()
        logDisplay.clear()
        results.value = 0
        searchText.text = ".*"
        if (logFile.isFile) {
            this.regex = regex
            val inputStream: InputStream = logFile.inputStream()
            var counter = 0
            inputStream.bufferedReader().forEachLine {
                if (it.uppercase().contains(regex)) {
                    counter++
                    val msgT: LogMessage = Json.decodeFromString(it)
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
