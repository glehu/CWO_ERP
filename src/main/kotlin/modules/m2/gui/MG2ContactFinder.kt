package modules.m2.gui

import api.logic.getCWOClient
import api.misc.json.M1EntryListJson
import db.CwODB
import interfaces.IModule
import io.ktor.client.request.*
import io.ktor.util.*
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.misc.SongPropertyMainDataModel
import modules.m2.Contact
import modules.m2.logic.M2Controller
import modules.m2.logic.M2DBManager
import modules.mx.activeUser
import modules.mx.isClientGlobal
import modules.mx.logic.MXLog
import modules.mx.logic.indexFormat
import modules.mx.m2GlobalIndex
import modules.mx.maxSearchResultsGlobal
import tornadofx.*
import kotlin.system.measureTimeMillis

@InternalAPI
@ExperimentalSerializationApi
class MG2ContactFinder : IModule, View("M2 Contacts") {
    override fun moduleNameLong() = "MG2ContactFinder"
    override fun module() = "M2"
    val db: CwODB by inject()
    private val m2Controller: M2Controller by inject()
    private val song: SongPropertyMainDataModel by inject()
    private var searchText: TextField by singleAssign()
    private var exactSearch: CheckBox by singleAssign()
    private var contactsFound: ObservableList<Contact> = observableListOf(Contact(-1, ""))
    private var ixNr = SimpleStringProperty()
    private val ixNrList = FXCollections.observableArrayList(m2Controller.getIndexUserSelection())!!
    private val threadIDCurrentProperty = SimpleIntegerProperty()
    private var threadIDCurrent by threadIDCurrentProperty
    override val root = borderpane {
        center = form {
            contactsFound.clear()
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
                    ixNr.value = "1-Name"
                    combobox(ixNr, ixNrList) {
                        tooltip("Selects the index file that will be searched in.")
                    }
                }
                tableview(contactsFound) {
                    readonlyColumn("ID", Contact::uID).prefWidth(65.0)
                    readonlyColumn("Name", Contact::name).prefWidth(350.0)
                    readonlyColumn("F.Name", Contact::firstName).prefWidth(250.0)
                    readonlyColumn("City", Contact::city).prefWidth(200.0)
                    onUserSelect(1) {
                        if (song.uID.value == -2) {
                            //Data transfer
                            song.uID.value = it.uID
                            song.name.value = it.name
                            song.commit()
                            close()
                        } else {
                            m2Controller.showContact(it)
                            searchText.text = ""
                            close()
                        }
                    }
                    isFocusTraversable = false
                }
            }
        }
    }

    private fun startSearch() {
        runAsync {
            threadIDCurrent++
            searchForContacts(threadIDCurrent)
        }
    }

    private fun searchForContacts(threadID: Int) {
        var entriesFound = 0
        val dbManager = M2DBManager()
        val timeInMillis = measureTimeMillis {
            if (!isClientGlobal) {
                contactsFound.clear()
                db.getEntriesFromSearchString(
                    indexFormat(searchText.text),
                    ixNr.value.substring(0, 1).toInt(),
                    exactSearch.isSelected,
                    module(),
                    maxSearchResultsGlobal,
                    m2GlobalIndex
                ) { _, bytes ->
                    //Add the contacts to the table
                    if (threadID == threadIDCurrent) {
                        contactsFound.add(dbManager.decodeEntry(bytes) as Contact)
                        entriesFound++
                    }
                }
            } else if (isClientGlobal) {
                if (searchText.text.isNotEmpty()) {
                    runBlocking {
                        launch {
                            val entryListJson: M1EntryListJson = getCWOClient(activeUser.username, activeUser.password)
                                .get(
                                    getApiUrl() +
                                            "entry/${indexFormat(searchText.text)}" +
                                            "?type=name"
                                )
                            if (threadID == threadIDCurrent) {
                                this@MG2ContactFinder.contactsFound.clear()
                                for (entryBytes: ByteArray in entryListJson.resultsList) {
                                    entriesFound++
                                    this@MG2ContactFinder.contactsFound.add(dbManager.decodeEntry(entryBytes) as Contact)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (threadID == threadIDCurrent) {
            MXLog.log(
                module(), MXLog.LogType.INFO, "$entriesFound contacts loaded (in $timeInMillis ms)",
                moduleNameLong()
            )
        }
    }
}