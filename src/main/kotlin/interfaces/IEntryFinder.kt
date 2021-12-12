package interfaces

import api.misc.json.EntryJson
import db.CwODB
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.*
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.mx.isClientGlobal
import modules.mx.logic.Log
import modules.mx.logic.indexFormat
import java.net.InetSocketAddress
import kotlin.system.measureTimeMillis

@InternalAPI
@ExperimentalSerializationApi
interface IEntryFinder : IModule {
    var searchText: TextField
    var exactSearch: CheckBox
    var entriesFound: ObservableList<IEntry>
    var ixNr: SimpleStringProperty
    val ixNrList: ObservableList<String>
    val threadIDCurrentProperty: SimpleIntegerProperty

    fun searchForEntries(
        threadID: Int,
    ) {
        var entriesFound = 0
        val timeInMillis = measureTimeMillis {
            if (!isClientGlobal) {
                CwODB.getEntriesFromSearchString(
                    searchText = indexFormat(searchText.text),
                    ixNr = ixNr.value.substring(0, 1).toInt(),
                    exactSearch = exactSearch.isSelected,
                    indexManager = getIndexManager()!!,
                ) { _, bytes ->
                    if (threadID == threadIDCurrentProperty.value) {
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
                }
            } else {
                /**
                 * ########## RAW SOCKET TCP DATA TRANSFER ##########
                 */
                if (searchText.text.isNotEmpty()) {
                    runBlocking {
                        val socket =
                            aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
                                .connect(InetSocketAddress("127.0.0.1", 2323))
                        val sockIn = socket.openReadChannel()
                        val sockOut = socket.openWriteChannel(autoFlush = true)
                        val exact = if (exactSearch.isSelected) {
                            "SPECIFIC"
                        } else "ALL"
                        sockOut.writeStringUtf8(
                            "IXS $module ${ixNr.value.substring(0, 1)} $exact NAME ${indexFormat(searchText.text)}\r\n"
                        )
                        val response = sockIn.readUTF8Line(Int.MAX_VALUE)
                        if (response == "RESULTS") {
                            if (threadID == threadIDCurrentProperty.value) {
                                this@IEntryFinder.entriesFound.clear()
                                var done = false
                                while (!done) {
                                    val inputLine = sockIn.readUTF8Line(Int.MAX_VALUE)
                                    if (inputLine != "DONE") {
                                        try {
                                            val entryJson = Json.decodeFromString<EntryJson>(inputLine!!)
                                            entriesFound++
                                            this@IEntryFinder.entriesFound.add(decode(entryJson.entry))
                                        } catch (e: Exception) {
                                            println("IXLOOK-ERR-${e.message}")
                                        }
                                    } else done = true
                                }
                            }
                        }
                    }
                }
            }
        }
        if (threadID == threadIDCurrentProperty.value) {
            if (entriesFound == 0) {
                this.entriesFound.clear()
            } else {
                log(Log.LogType.INFO, "$entriesFound results (total $timeInMillis ms)")
            }
        }
    }
}
