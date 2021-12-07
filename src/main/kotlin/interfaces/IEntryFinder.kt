package interfaces

import api.logic.getTokenClient
import api.misc.json.EntryBytesListJson
import db.CwODB
import io.ktor.client.request.*
import io.ktor.util.*
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.isClientGlobal
import modules.mx.logic.MXLog
import modules.mx.logic.indexFormat
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

    fun searchForEntries(threadID: Int) {
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
                            println("IXLOOK-ERR-${e.message}")
                        }
                    }
                }
            } else {
                if (searchText.text.isNotEmpty()) {
                    runBlocking {
                        launch {
                            var url = getApiUrl() +
                                    "entry/${indexFormat(searchText.text)}" +
                                    "?type=name"
                            if (exactSearch.isSelected) {
                                url += "&index=" + ixNr.value.substring(0, 1)
                            }
                            val entryBytesListJson: EntryBytesListJson = getTokenClient().get(url)
                            if (threadID == threadIDCurrentProperty.value) {
                                this@IEntryFinder.entriesFound.clear()
                                for (entryBytes: ByteArray in entryBytesListJson.resultsList) {
                                    entriesFound++
                                    this@IEntryFinder.entriesFound.add(decode(entryBytes))
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
                log(MXLog.LogType.INFO, "$entriesFound results (total $timeInMillis ms)")
            }
        }
    }
}
