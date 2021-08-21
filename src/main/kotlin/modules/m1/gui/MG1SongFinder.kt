package modules.m1.gui

import db.CwODB
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import interfaces.IModule
import modules.m1.Song
import modules.m1.logic.M1Controller
import modules.m1.logic.M1DBManager
import modules.m2.logic.M2Controller
import modules.mx.logic.MXLog
import modules.mx.m1GlobalIndex
import modules.mx.maxSearchResultsGlobal
import tornadofx.*
import kotlin.system.measureTimeMillis

@ExperimentalSerializationApi
class MG1SongFinder : IModule, View("M1 Songs")
{
    override fun moduleNameLong() = "MG1SongFinder"
    override fun module() = "M1"
    val db: CwODB by inject()
    private val m1Controller: M1Controller by inject()
    private val m2Controller: M2Controller by inject()
    private var searchText: TextField by singleAssign()
    private var exactSearch: CheckBox by singleAssign()
    private var songsFound: ObservableList<Song> = observableListOf(Song(-1, ""))
    private var ixNr = SimpleStringProperty()
    private val ixNrList = FXCollections.observableArrayList(m1GlobalIndex.getIndexUserSelection())!!
    private val threadIDCurrentProperty = SimpleIntegerProperty()
    private var threadIDCurrent by threadIDCurrentProperty
    override val root = borderpane {
        center = form {
            songsFound.clear()
            threadIDCurrent = 0
            fieldset {
                field("Search") {
                    searchText = textfield {
                        textProperty().addListener { _, _, _ ->
                            startSearch()
                        }
                        tooltip("Contains the search text that will be used to find an entry.")
                    }
                    exactSearch = checkbox("Exact Search") {
                        tooltip("If checked, a literal search will be done.")
                    }
                }
                fieldset("Index")
                {
                    ixNr.value = ixNrList[0]
                    combobox(ixNr, ixNrList) {
                        tooltip("Selects the index file that will be searched in.")
                    }
                }
                tableview(songsFound) {
                    readonlyColumn("ID", Song::uID).prefWidth(65.0)
                    readonlyColumn("Name", Song::name).prefWidth(310.0)
                    readonlyColumn("Vocalist", Song::vocalist).prefWidth(200.0).cellFormat {
                        text = m2Controller.getContactName(rowItem.vocalistUID, rowItem.vocalist)
                        rowItem.vocalist = text
                        textFill = Color.LIGHTGRAY
                    }
                    readonlyColumn("Producer", Song::producer).prefWidth(200.0).cellFormat {
                        text = m2Controller.getContactName(rowItem.producerUID, rowItem.producer)
                        rowItem.producer = text
                        textFill = Color.LIGHTGRAY
                    }
                    readonlyColumn("Genre", Song::genre).prefWidth(200.0)
                    onUserSelect(1) {
                        m1Controller.showSong(it)
                        //startSearch()
                        searchText.text = ""
                        close()
                    }
                    isFocusTraversable = false
                }
            }
        }
    }

    private fun startSearch()
    {
        runAsync {
            threadIDCurrent++
            searchForSongs(threadIDCurrent)
        }
    }

    private fun searchForSongs(threadID: Int)
    {
        var entriesFound = 0
        val timeInMillis = measureTimeMillis {
            val dbManager = M1DBManager()
            db.getEntriesFromSearchString(
                searchText.text.uppercase(),
                ixNr.value.substring(0, 1).toInt(),
                exactSearch.isSelected,
                module(),
                maxSearchResultsGlobal,
                m1GlobalIndex
            ) { _, bytes ->
                if (threadID == threadIDCurrent)
                {
                    if (entriesFound == 0) songsFound.clear()
                    songsFound.add(dbManager.decodeEntry(bytes) as Song)
                    entriesFound++
                }
            }
        }
        if (threadID == threadIDCurrent)
        {
            if (entriesFound == 0)
            {
                songsFound.clear()
            } else
            {
                MXLog.log(
                    module(), MXLog.LogType.INFO, "$entriesFound songs loaded (in $timeInMillis ms)",
                    moduleNameLong()
                )
            }
        }
    }
}