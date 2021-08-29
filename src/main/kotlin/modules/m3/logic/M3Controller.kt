package modules.m3.logic

import api.logic.getCWOClient
import db.CwODB
import interfaces.IModule
import io.ktor.client.request.*
import io.ktor.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m3.Invoice
import modules.m3.gui.InvoiceConfiguratorWizard
import modules.m3.gui.InvoiceViewerWizard
import modules.m3.misc.InvoiceProperty
import modules.m3.misc.getInvoiceFromInvoiceProperty
import modules.m3.misc.getInvoicePropertyFromInvoice
import modules.mx.activeUser
import modules.mx.isClientGlobal
import modules.mx.m1GlobalIndex
import modules.mx.m3GlobalIndex
import tornadofx.Controller

@InternalAPI
@ExperimentalSerializationApi
class M3Controller : IModule, Controller() {
    override fun moduleNameLong() = "M3Controller"
    override fun module() = "M3"

    private val wizard = find<InvoiceConfiguratorWizard>()
    val db: CwODB by inject()

    fun openWizardNewInvoice() {
        wizard.invoice.item = InvoiceProperty()
        wizard.isComplete = false
        wizard.onComplete {
            if (wizard.invoice.seller.value !== null) {
                val raf = db.openRandomFileAccess(module(), CwODB.RafMode.READWRITE)
                M3DBManager().saveEntry(
                    getInvoiceFromInvoiceProperty(wizard.invoice.item), db, -1L, -1, raf, m3GlobalIndex
                )
                db.closeRandomFileAccess(raf)
                wizard.invoice.item = InvoiceProperty()
                wizard.isComplete = false
                wizard.close()
            }
        }
        wizard.openModal(block = true)
    }

    fun showInvoice(invoice: Invoice) {
        val wizard = find<InvoiceViewerWizard>()
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
                    indexManager = m3GlobalIndex
                )
                this.db.closeRandomFileAccess(raf)
                wizard.invoice.item = InvoiceProperty()
                wizard.isComplete = false
            }
        }
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