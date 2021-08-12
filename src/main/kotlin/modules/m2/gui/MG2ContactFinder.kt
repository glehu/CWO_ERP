package modules.m2.gui

import db.CwODB
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import kotlinx.serialization.ExperimentalSerializationApi
import modules.IModule
import modules.m1.misc.SongModelP1
import modules.m2.Contact
import modules.m2.logic.M2Controller
import modules.m2.logic.M2DBManager
import modules.mx.logic.MXLog
import modules.mx.m2GlobalIndex
import modules.mx.maxSearchResultsGlobal
import modules.mx.rightButtonsWidth
import tornadofx.*
import kotlin.system.measureTimeMillis

@ExperimentalSerializationApi
class MG2ContactFinder : IModule, View("M2 Contacts")
{
    override fun moduleNameLong() = "MG2ContactFinder"
    override fun module() = "M2"
    val db: CwODB by inject()
    private val m2Controller: M2Controller by inject()
    private val song: SongModelP1 by inject()
    private var searchText: TextField by singleAssign()
    private var exactSearch: CheckBox by singleAssign()
    private var contactsFound: ObservableList<Contact> = observableListOf(Contact(-1, ""))
    private var ixNr = SimpleStringProperty()
    private val ixNrList = FXCollections.observableArrayList(m2GlobalIndex.getIndexUserSelection())!!
    private val threadIDCurrent = SimpleIntegerProperty()
    override val root = borderpane {
        center = form {
            contactsFound.clear()
            threadIDCurrent.value = 0
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
                        if (song.uID.value == -2)
                        {
                            //Data transfer
                            song.uID.value = it.uID
                            song.name.value = it.name
                            song.commit()
                            close()
                        } else
                        {
                            m2Controller.showContact(it)
                            startSearch()
                        }
                    }
                }
            }
        }
        right = vbox {
            //Main functions
            button("New Contact") {
                action {
                    m2Controller.openWizardNewContact()
                    startSearch()
                }
                tooltip("Add a new contact to the database.")
                prefWidth = rightButtonsWidth
            }
            //Analytics functions
            button("Analytics") {
                action { m2Controller.openAnalytics() }
                tooltip("Display a chart to show the distribution of genres.")
                prefWidth = rightButtonsWidth
            }
            //Maintenance functions
            button("Rebuild indices") {
                //TODO: Not yet implemented
                isDisable = true
                tooltip("Rebuilds all indices in case of faulty indices.")
                prefWidth = rightButtonsWidth
            }
            //Data import
            button("Data Import") {
                action { m2Controller.openDataImport() }
                tooltip("Import contact data from a .csv file.")
                prefWidth = rightButtonsWidth
            }
        }
    }

    private fun startSearch()
    {
        runAsync {
            threadIDCurrent.value++
            searchForContacts(threadIDCurrent.value)
        }
    }

    private fun searchForContacts(threadID: Int)
    {
        var entriesFound = 0
        val timeInMillis = measureTimeMillis {
            val dbManager = M2DBManager()
            contactsFound.clear()
            db.getEntriesFromSearchString(
                searchText.text.uppercase(),
                ixNr.value.substring(0, 1).toInt(),
                exactSearch.isSelected,
                module(),
                maxSearchResultsGlobal,
                m2GlobalIndex
            ) { _, bytes ->
                //Add the contacts to the table
                if (threadID == threadIDCurrent.value)
                {
                    contactsFound.add(dbManager.decodeEntry(bytes) as Contact)
                    entriesFound++
                }
            }
        }
        if (threadID == threadIDCurrent.value)
        {
            MXLog.log(
                module(), MXLog.LogType.INFO, "$entriesFound contacts loaded (in $timeInMillis ms)",
                moduleNameLong()
            )
        }
    }
}