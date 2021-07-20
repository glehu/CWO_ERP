package modules.m1.gui

import db.CwODB
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import kotlinx.serialization.ExperimentalSerializationApi
import modules.IModule
import modules.m1.Song
import modules.m1.misc.SongProperty
import modules.m1.logic.M1DBManager
import modules.m1.logic.M1IndexManager
import modules.m1.misc.getSongFromProperty
import modules.m1.misc.getSongPropertyFromSong
import modules.mx.logic.MXLog
import modules.mx.maxSearchResultsGlobal
import tornadofx.*
import kotlin.system.measureTimeMillis

@ExperimentalSerializationApi
class SongController : IModule, Controller()
{
    override fun moduleName() = "MG1UserInterface"

    private val wizard = find<SongConfiguratorWizard>()
    val db: CwODB by inject()
    val indexManager: M1IndexManager by inject(Scope(db))

    fun openWizardNewSong()
    {
        wizard.song.item = SongProperty()
        wizard.isComplete = false
        wizard.onComplete {
            if (wizard.song.item.nameProperty.value !== null)
            {
                val raf = db.openRandomFileAccess("M1", "rw")
                M1DBManager().saveEntry(getSongFromProperty(wizard.song.item), db, -1L, -1, raf, indexManager)
                db.closeRandomFileAccess(raf)
                wizard.song.item = SongProperty()
                wizard.isComplete = false
                wizard.close()
            }
        }
        wizard.openModal()
    }

    fun openWizardFindSong()
    {
        find(SongFinder::class, Scope(indexManager)).openModal()
    }

    fun openAnalytics()
    {
        //TODO: Add multiple analytics modes
        find(MG1Analytics::class, Scope(indexManager)).openModal()
    }
}

@ExperimentalSerializationApi
class SongFinder : IModule, View("Find Song")
{
    override fun moduleName() = "MG1UserInterface"
    val db: CwODB by inject()
    val indexManager: M1IndexManager by inject()
    private var songName: TextField by singleAssign()
    private var exactSearch: CheckBox by singleAssign()
    private var songsFound: ObservableList<Song> = observableList(Song(-1, ""))
    private var ixNr = SimpleStringProperty()
    private val ixNrList = FXCollections.observableArrayList(indexManager.getIndexUserSelection())!!
    private val threadIDCurrentProperty = SimpleIntegerProperty()
    private var threadIDCurrent by threadIDCurrentProperty
    override val root = form {
        songsFound.clear()
        threadIDCurrent = 0
        fieldset {
            field("Song Name") {
                songName = textfield {
                    textProperty().addListener { _, _, _ ->
                        runAsync {
                            threadIDCurrent++
                            searchForSongs(threadIDCurrent, indexManager)
                        }
                    }
                    tooltip("Contains the search text that will be used to find an entry.")
                }
                exactSearch = checkbox("Exact Search") {
                    tooltip("If checked, a literal search will be done.")
                }
            }
            fieldset("Index")
            {
                ixNr.value = "1-Name"
                combobox(ixNr, ixNrList) {
                    tooltip("Selects the index file that will be searched in.")
                }
            }
            tableview(songsFound) {
                readonlyColumn("ID", Song::uID).prefWidth(65.0)
                readonlyColumn("Name", Song::name).prefWidth(310.0)
                readonlyColumn("Vocalist", Song::vocalist).prefWidth(200.0)
                readonlyColumn("Producer", Song::producer).prefWidth(200.0)
                readonlyColumn("Genre", Song::genre).prefWidth(200.0)
                onUserSelect(1) {
                    showSong(it)
                    songsFound.clear()
                    close()
                }
            }
        }
    }

    private fun searchForSongs(threadID: Int, indexManager: M1IndexManager)
    {
        var entriesFound = 0
        val timeInMillis = measureTimeMillis {
            val dbManager = M1DBManager()
            db.getEntriesFromSearchString(
                songName.text.uppercase(),
                ixNr.value.substring(0, 1).toInt(),
                exactSearch.isSelected,
                "M1",
                maxSearchResultsGlobal,
                indexManager
            ) { _, bytes ->
                if (threadID >= threadIDCurrent)
                {
                    if (entriesFound == 0) songsFound.clear()
                    songsFound.add(dbManager.getEntry(bytes) as Song)
                    entriesFound++
                }
            }
        }
        if (threadID >= threadIDCurrent)
        {
            if (entriesFound == 0)
            {
                songsFound.clear()
            } else
            {
                MXLog.log(
                    "M1", MXLog.LogType.INFO, "$entriesFound songs loaded (in $timeInMillis ms)",
                    moduleName()
                )
            }
        }
    }

    private fun showSong(song: Song)
    {
        val wizard = find<SongViewerWizard>()
        wizard.song.item = getSongPropertyFromSong(song)
        wizard.onComplete {
            if (wizard.song.item !== null)
            {
                val raf = db.openRandomFileAccess("M1", "rw")
                M1DBManager().saveEntry(
                    entry = getSongFromProperty(wizard.song.item),
                    cwodb = db,
                    posDB = indexManager.indexList[0]!!.indexMap[wizard.song.item.uniqueID]!!.pos,
                    byteSize = indexManager.indexList[0]!!.indexMap[wizard.song.item.uniqueID]!!.byteSize,
                    raf = raf,
                    indexManager = indexManager
                )
                db.closeRandomFileAccess(raf)
                wizard.song.item = SongProperty()
                wizard.isComplete = false
                wizard.close()
            }
        }
        wizard.openModal()
    }
}