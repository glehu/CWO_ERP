package modules.m3.logic

import api.logic.getCWOClient
import api.misc.json.M2EntryJson
import api.misc.json.M3EntryListJson
import db.CwODB
import interfaces.IModule
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import javafx.collections.ObservableList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import modules.m3.Invoice
import modules.m3.M3Item
import modules.m3.gui.InvoiceConfiguratorWizard
import modules.m3.gui.ItemConfiguratorWizard
import modules.m3.gui.MG3InvoiceFinder
import modules.m3.misc.*
import modules.mx.activeUser
import modules.mx.isClientGlobal
import modules.mx.m3GlobalIndex
import modules.mx.maxSearchResultsGlobal
import tornadofx.Controller
import tornadofx.observableListOf

@InternalAPI
@ExperimentalSerializationApi
class M3Controller : IModule, Controller() {
    override fun moduleNameLong() = "M3Controller"
    override fun module() = "M3"

    private val wizard = find<InvoiceConfiguratorWizard>()
    val db: CwODB by inject()

    val client = getCWOClient(activeUser.username, activeUser.password)

    fun searchEntry() {
        find<MG3InvoiceFinder>().openModal()
    }

    fun newEntry() {
        wizard.invoice.item = InvoiceProperty()
        wizard.invoice.validate()
        wizard.isComplete = false
    }

    fun saveEntry() {
        var isComplete = true
        wizard.invoice.commit()
        if (!wizard.invoice.isValid) isComplete = false
        if (isComplete) {
            if (!isClientGlobal) {
                val raf = db.openRandomFileAccess(module(), CwODB.RafMode.READWRITE)
                wizard.invoice.uID.value = M3DBManager().saveEntry(
                    entry = getInvoiceFromInvoiceProperty(wizard.invoice.item),
                    cwodb = db,
                    posDB = -1L,
                    byteSize = -1,
                    raf = raf,
                    indexManager = m3GlobalIndex,
                    indexWriteToDisk = true,
                    userName = activeUser.username
                )
                db.closeRandomFileAccess(raf)
            } else {
                val entry = getInvoiceFromInvoiceProperty(wizard.invoice.item)
                runBlocking {
                    launch {
                        wizard.invoice.uID.value =
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

    fun showInvoice(invoice: Invoice) {
        val wizard = find<InvoiceConfiguratorWizard>()
        wizard.invoice.item = getInvoicePropertyFromInvoice(invoice)
        wizard.onComplete {
            if (wizard.invoice.uID.value != -1) {
                val raf = db.openRandomFileAccess(module(), CwODB.RafMode.READWRITE)
                M3DBManager().saveEntry(
                    entry = getInvoiceFromInvoiceProperty(wizard.invoice.item),
                    cwodb = db,
                    posDB = m3GlobalIndex.indexList[0]!!.indexMap[wizard.invoice.item.uID]!!.pos,
                    byteSize = m3GlobalIndex.indexList[0]!!.indexMap[wizard.invoice.item.uID]!!.byteSize,
                    raf = raf,
                    indexManager = m3GlobalIndex,
                    indexWriteToDisk = true,
                    userName = activeUser.username
                )
                this.db.closeRandomFileAccess(raf)
                wizard.invoice.item = InvoiceProperty()
                wizard.isComplete = false
            }
        }
    }

    fun getEntryBytesListJson(searchText: String, ixNr: Int): M3EntryListJson {
        val resultsListJson = M3EntryListJson(0, arrayListOf())
        var resultCounter = 0
        db.getEntriesFromSearchString(
            searchText = searchText.uppercase(),
            ixNr = ixNr,
            exactSearch = false,
            module = module(),
            maxSearchResults = maxSearchResultsGlobal,
            indexManager = m3GlobalIndex
        ) { _, bytes ->
            resultCounter++
            resultsListJson.resultsList.add(bytes)
        }
        resultsListJson.resultsAmount = resultCounter
        return resultsListJson
    }

    fun getEntryBytes(uID: Int): ByteArray {
        return if (uID != -1) {
            db.getEntryFromUniqueID(uID, module(), m3GlobalIndex.indexList[0]!!)
        } else byteArrayOf()
    }

    fun getIndexUserSelection(): ArrayList<String> {
        lateinit var indexUserSelection: ArrayList<String>
        if (!isClientGlobal) {
            indexUserSelection = m3GlobalIndex.getIndexUserSelection()
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

    fun createAndReturnItem(): M3Item {
        val item = M3Item(-1, "")
        val wizard = ItemConfiguratorWizard()
        wizard.showHeader = false
        wizard.showSteps = false
        wizard.item.item = getItemPropertyFromItem(item)
        wizard.openModal(block = true)
        return getItemFromItemProperty(wizard.item.item)
    }

    fun getItemsFromInvoiceProperty(invoiceProperty: InvoiceProperty): ObservableList<M3Item> {
        val items: ObservableList<M3Item> = observableListOf(M3Item(-1, ""))
        items.clear()
        val invoice = getInvoiceFromInvoiceProperty(invoiceProperty)
        for ((_, v) in invoice.items) {
            if (v.isNotEmpty()) items.add(Json.decodeFromString(v))
        }
        return items
    }
}