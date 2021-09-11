package interfaces

import api.logic.getCWOClient
import api.misc.json.EntryListJson
import db.CwODB
import io.ktor.client.request.*
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
                        if (entriesFound == 0) this.entriesFound.clear()
                        this.entriesFound.add(decode(bytes))
                        entriesFound++
                    }
                }
            } else if (isClientGlobal) {
                if (searchText.text.isNotEmpty()) {
                    runBlocking {
                        launch {
                            val entryListJson: EntryListJson = getCWOClient()
                                .get(
                                    getApiUrl() +
                                            "entry/${indexFormat(searchText.text)}" +
                                            "?type=name"
                                )
                            if (threadID == threadIDCurrentProperty.value) {
                                this@IEntryFinder.entriesFound.clear()
                                for (entryBytes: ByteArray in entryListJson.resultsList) {
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
                log(MXLog.LogType.INFO, "$entriesFound entries loaded (in $timeInMillis ms)")
            }
        }
    }
}