package modules.m2.logic

import api.logic.getCWOClient
import api.misc.json.M1EntryJson
import api.misc.json.M2EntryJson
import api.misc.json.M2EntryListJson
import db.CwODB
import interfaces.IModule
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import modules.m1.misc.SongPropertyMainDataModel
import modules.m2.Contact
import modules.m2.gui.ContactConfiguratorWizard
import modules.m2.gui.MG2Analytics
import modules.m2.gui.MG2ContactFinder
import modules.m2.gui.MG2Import
import modules.m2.misc.ContactProperty
import modules.m2.misc.getContactFromProperty
import modules.m2.misc.getContactPropertyFromContact
import modules.mx.*
import tornadofx.Controller
import tornadofx.Scope
import tornadofx.find

@InternalAPI
@ExperimentalSerializationApi
class M2Controller : IModule, Controller() {
    override fun moduleNameLong() = "M2Controller"
    override fun module() = "M2"

    private val wizard = find<ContactConfiguratorWizard>()
    val db: CwODB by inject()

    val client = getCWOClient(activeUser.username, activeUser.password)

    fun searchEntry() {
        find<MG2ContactFinder>().openModal()
    }

    fun saveEntry() {
        var isComplete = true
        wizard.contact.commit()
        if (!wizard.contact.isValid) isComplete = false
        if (isComplete) {
            if (!isClientGlobal) {
                val raf = db.openRandomFileAccess(module(), CwODB.RafMode.READWRITE)
                wizard.contact.uID.value = M2DBManager().saveEntry(
                    getContactFromProperty(wizard.contact.item), db, -1L, -1, raf, m2GlobalIndex
                )
                db.closeRandomFileAccess(raf)
            } else {
                val entry = getContactFromProperty(wizard.contact.item)
                runBlocking {
                    launch {
                        wizard.contact.uID.value =
                            client.post("${getApiUrl()}saveentry") {
                                contentType(ContentType.Application.Json)
                                body = M2EntryJson(entry.uID, ProtoBuf.encodeToByteArray(entry))
                            }
                    }
                }
            }
            wizard.isComplete = false
        }
    }

    fun newEntry() {
        wizard.contact.item = ContactProperty()

        wizard.contact.validate()
        wizard.isComplete = false
    }

    fun openAnalytics() {
        //TODO: Add multiple analytics modes
        find<MG2Analytics>().openModal()
    }

    fun openDataImport() {
        find<MG2Import>().openModal()
    }

    fun selectAndReturnContact(): Contact {
        val contact: Contact
        val newScope = Scope()
        val dataTransfer = SongPropertyMainDataModel()
        dataTransfer.uID.value = -2
        setInScope(dataTransfer, newScope)
        find<MG2ContactFinder>(newScope).openModal(block = true)
        contact = if (dataTransfer.name.value != null) {
            M2DBManager().getEntry(
                dataTransfer.uID.value, db, m2GlobalIndex.indexList[0]!!
            ) as Contact
        } else Contact(-1, "")
        return contact
    }

    fun getEntryBytes(uID: Int): ByteArray {
        return if (uID != -1) {
            db.getEntryFromUniqueID(uID, module(), m2GlobalIndex.indexList[0]!!)
        } else byteArrayOf()
    }

    fun getContactName(uID: Int, default: String): String {
        return if (uID != -1) {
            getContact(uID).name
        } else default
    }

    private fun getContact(uID: Int): Contact {
        lateinit var contact: Contact
        if (uID != -1) {
            if (!isClientGlobal) {
                contact = M2DBManager().getEntry(
                    uID, db, m2GlobalIndex.indexList[0]!!
                ) as Contact
            } else {
                runBlocking {
                    launch {
                        contact = M2DBManager().decodeEntry(
                            client.get(
                                "${getApiUrl()}entry/$uID?type=uid"
                            )
                        ) as Contact
                    }
                }
            }
        } else contact = Contact(-1, "")
        return contact
    }

    fun showContact(uID: Int) {
        showContact(getContact(uID))
    }

    fun showContact(contact: Contact) {
        val wizard = find<ContactConfiguratorWizard>()
        wizard.contact.item = getContactPropertyFromContact(contact)
        wizard.onComplete {
            if (wizard.contact.uID.value != -1) {
                val raf = db.openRandomFileAccess(module(), CwODB.RafMode.READWRITE)
                M2DBManager().saveEntry(
                    entry = getContactFromProperty(wizard.contact.item),
                    cwodb = db,
                    posDB = m2GlobalIndex.indexList[0]!!.indexMap[wizard.contact.item.uID]!!.pos,
                    byteSize = m2GlobalIndex.indexList[0]!!.indexMap[wizard.contact.item.uID]!!.byteSize,
                    raf = raf,
                    indexManager = m2GlobalIndex
                )
                this.db.closeRandomFileAccess(raf)
                wizard.contact.item = ContactProperty()
                wizard.isComplete = false
            }
        }
    }

    fun getEntryBytesListJson(searchText: String, ixNr: Int): M2EntryListJson {
        val resultsListJson = M2EntryListJson(0, arrayListOf())
        var resultCounter = 0
        db.getEntriesFromSearchString(
            searchText = searchText.uppercase(),
            ixNr = ixNr,
            exactSearch = false,
            module = module(),
            maxSearchResults = maxSearchResultsGlobal,
            indexManager = m2GlobalIndex
        ) { _, bytes ->
            resultCounter++
            resultsListJson.resultsList.add(bytes)
        }
        resultsListJson.resultsAmount = resultCounter
        return resultsListJson
    }

    fun getIndexUserSelection(): ArrayList<String> {
        lateinit var indexUserSelection: ArrayList<String>
        if (!isClientGlobal) {
            indexUserSelection = m1GlobalIndex.getIndexUserSelection()
        } else {
            runBlocking {
                launch {
                    indexUserSelection =
                        getCWOClient(activeUser.username, activeUser.password)
                            .get("${getApiUrl()}indexselection")
                }
            }
        }
        return indexUserSelection
    }
}