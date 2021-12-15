package interfaces

import api.misc.json.EntryJson
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
import modules.mx.isClientGlobal
import modules.mx.logic.Log
import modules.mx.logic.indexFormat
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

    fun searchForEntries(
        /**
         * References to the EntryFinder GUI instance carrying the search data.
         */
        entryFinder: IEntryFinder
    ) {
        var entriesFound = 0
        if (entryFinder.searchText.text.isEmpty()) {
            this@IEntryFinder.entriesFound.clear()
            return
        }
        if (!isClientGlobal) {
            CwODB.getEntriesFromSearchString(
                searchText = indexFormat(entryFinder.searchText.text),
                ixNr = entryFinder.ixNr.value.substring(0, 1).toInt(),
                exactSearch = entryFinder.exactSearch.isSelected,
                indexManager = getIndexManager()!!,
            ) { _, bytes ->
                if (entriesFound == 0) {
                    this.entriesFound.clear()
                }
                try {
                    this.entriesFound.add(decode(bytes))
                    entriesFound++
                } catch (e: Exception) {
                    log(Log.LogType.ERROR, "IXLOOK-ERR-${e.message}")
                }
            }
        } else {
            /**
             * ########## RAW SOCKET TCP DATA TRANSFER ##########
             */
            runBlocking {
                val socket =
                    aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
                        .connect(InetSocketAddress("127.0.0.1", 2323))
                val sockIn = socket.openReadChannel()
                val sockOut = socket.openWriteChannel(autoFlush = true)
                val exact = if (entryFinder.exactSearch.isSelected) {
                    "SPECIFIC"
                } else "ALL"
                sockOut.writeStringUtf8(
                    "IXS $module ${
                        ixNr.value.substring(0, 1)
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
}
