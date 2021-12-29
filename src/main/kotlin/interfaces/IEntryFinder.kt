package interfaces

import api.misc.json.EntryJson
import components.gui.tornadofx.entryfinder.EntryFinderSearchMask
import db.CwODB
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.*
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.mx.Ini
import modules.mx.getIniFile
import modules.mx.isClientGlobal
import modules.mx.logic.Log
import modules.mx.logic.indexFormat
import modules.mx.maxSearchResultsGlobal
import java.net.InetSocketAddress

@InternalAPI
@ExperimentalSerializationApi
interface IEntryFinder : IModule {
    var searchText: TextField
    var exactSearch: CheckBox
    var entriesFound: ObservableList<IEntry>
    var ixNr: SimpleStringProperty
    val ixNrList: ObservableList<String>
    val table: TableView<IEntry>
    val entryFinderSearchMask: EntryFinderSearchMask

    fun searchForEntries(
        /**
         * References to the EntryFinder GUI instance carrying the search data.
         */
        entryFinder: EntryFinderSearchMask
    ) {
        var entriesFound = 0
        if (entryFinder.searchText.text.isEmpty()) {
            try {
                this@IEntryFinder.entriesFound.clear()
            } catch (e: Exception) {
                log(Log.LogType.ERROR, e.message ?: "CLEAR ERR")
            }
            return
        }
        if (!isClientGlobal) {
            val overrideMaxSearchResultsGlobal =
                if (entryFinder.showAll.isSelected) -1 else maxSearchResultsGlobal
            CwODB.getEntriesFromSearchString(
                searchText = indexFormat(entryFinder.searchText.text),
                ixNr = entryFinder.ixNr.value.substring(0, 1).toInt(),
                exactSearch = entryFinder.exactSearch.isSelected,
                indexManager = getIndexManager()!!,
                maxSearchResults = overrideMaxSearchResultsGlobal
            ) { _, bytes ->
                if (entriesFound == 0) this.entriesFound.clear()
                try {
                    this.entriesFound.add(decode(bytes))
                    entriesFound++
                } catch (e: Exception) {
                    log(Log.LogType.ERROR, "IXLOOK-ERR-${e.message}")
                }
            }
        } else {
            // ########## RAW SOCKET TCP DATA TRANSFER ##########
            runBlocking {
                val iniVal = Json.decodeFromString<Ini>(getIniFile().readText())
                val socket =
                    aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect(
                        InetSocketAddress(
                            iniVal.telnetServerIPAddress.substringBefore(':'),
                            iniVal.telnetServerIPAddress.substringAfter(':').toInt()
                        )
                    )
                val sockIn = socket.openReadChannel()
                val sockOut = socket.openWriteChannel(autoFlush = true)
                val exact = if (entryFinder.exactSearch.isSelected) {
                    "SPECIFIC"
                } else "ALL"
                sockOut.writeStringUtf8(
                    "IXS $module ${
                        entryFinder.ixNr.value.substring(0, 1)
                    } $exact NAME ${indexFormat(entryFinder.searchText.text)}\r\n"
                )
                var response: String? = ""
                // Remove the HEY welcome message
                for (i in 0..2) {
                    response = sockIn.readUTF8Line()
                    println(response)
                    if (response == "HEY") {
                        response = sockIn.readUTF8Line()
                        println(response)
                        break
                    }
                }
                // Continue with results
                if (response == "RESULTS") {
                    this@IEntryFinder.entriesFound.clear()
                    var done = false
                    var inputLine = ""
                    while (!done) {
                        withTimeoutOrNull(1000) {
                            inputLine = sockIn.readUTF8Line()!!
                        }
                        if (inputLine != "DONE") {
                            try {
                                val entryJson = Json.decodeFromString<EntryJson>(inputLine)
                                entriesFound++
                                this@IEntryFinder.entriesFound.add(decode(entryJson.entry))
                            } catch (e: Exception) {
                                println("IXLOOK-ERR-${e.message} FOR $inputLine")
                            }
                        } else done = true
                    }
                }
            }
        }
        if (entriesFound == 0) this@IEntryFinder.entriesFound.clear()
    }

    fun modalSearch(searchText: String, ixNr: Int? = null) {
        if (ixNr != null && ixNr > 0) {
            entryFinderSearchMask.exactSearch.isSelected = true
            entryFinderSearchMask.ixNr.value = entryFinderSearchMask.ixNrList[ixNr - 1]
        }
        entryFinderSearchMask.searchText.text = searchText
        entryFinderSearchMask.startLookup()
    }

    /**
     * Tries to retrieve an index manager
     * @return the index manager if the software's instance is in server mode. Returns null if it's in client mode.
     */
    fun tryGetIndexManager(): IIndexManager? {
        return if (!isClientGlobal) getIndexManager() else null
    }
}
