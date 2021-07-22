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
import modules.m1.misc.SongModel
import modules.m2.Contact
import modules.m2.logic.M2DBManager
import modules.m2.logic.M2IndexManager
import modules.m2.misc.ContactProperty
import modules.m2.misc.getContactFromProperty
import modules.m2.misc.getContactPropertyFromContact
import modules.mx.logic.MXLog
import modules.mx.maxSearchResultsGlobal
import tornadofx.*
import kotlin.system.measureTimeMillis

@ExperimentalSerializationApi
class MG2ContactFinder : IModule, View("Find Contact")
{
    override fun moduleNameLong() = "MG2ContactFinder"
    override fun module() = "M2"
    val db: CwODB by inject()
    val indexManager: M2IndexManager by inject()
    private val song: SongModel by inject()
    private var contactName: TextField by singleAssign()
    private var exactSearch: CheckBox by singleAssign()
    private var contactsFound: ObservableList<Contact> = observableList(Contact(-1, ""))
    private var ixNr = SimpleStringProperty()
    private val ixNrList = FXCollections.observableArrayList(indexManager.getIndexUserSelection())!!
    private val threadIDCurrent = SimpleIntegerProperty()
    override val root = form {
        contactsFound.clear()
        threadIDCurrent.value = 0
        fieldset {
            field("Contact Name") {
                contactName = textfield {
                    textProperty().addListener { _, _, _ ->
                        runAsync {
                            threadIDCurrent.value++
                            searchForContacts(threadIDCurrent.value)
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
                    } else
                    {
                        showContact(it)
                        contactsFound.clear()
                        contactName.text = ""
                    }
                    close()
                }
            }
        }
    }

    private fun searchForContacts(threadID: Int)
    {
        var entriesFound = 0
        val timeInMillis = measureTimeMillis {
            val dbManager = M2DBManager()
            contactsFound.clear()
            db.getEntriesFromSearchString(
                contactName.text.uppercase(),
                ixNr.value.substring(0, 1).toInt(),
                exactSearch.isSelected,
                module(),
                maxSearchResultsGlobal,
                indexManager
            ) { _, bytes ->
                //Add the contacts to the table
                if (threadID >= threadIDCurrent.value)
                {
                    contactsFound.add(dbManager.decodeEntry(bytes) as Contact)
                    entriesFound++
                }
            }
        }
        if (threadID >= threadIDCurrent.value)
        {
            MXLog.log(
                module(), MXLog.LogType.INFO, "$entriesFound contacts loaded (in $timeInMillis ms)",
                moduleNameLong()
            )
        }
    }

    private fun showContact(contact: Contact)
    {
        val wizard = find<ContactViewerWizard>()
        wizard.contact.item = getContactPropertyFromContact(contact)
        wizard.onComplete {
            if (wizard.contact.item !== null)
            {
                val raf = db.openRandomFileAccess(module(), "rw")
                M2DBManager().saveEntry(
                    entry = getContactFromProperty(wizard.contact.item),
                    cwodb = db,
                    posDB = indexManager.indexList[0]!!.indexMap[wizard.contact.item.uID]!!.pos,
                    byteSize = indexManager.indexList[0]!!.indexMap[wizard.contact.item.uID]!!.byteSize,
                    raf = raf,
                    indexManager = indexManager
                )
                this.db.closeRandomFileAccess(raf)
                wizard.contact.item = ContactProperty()
                wizard.isComplete = false
                wizard.close()
            }
        }

        wizard.openModal()
    }
}