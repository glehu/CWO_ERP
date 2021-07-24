package modules.m3.logic

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import modules.IModule
import modules.m2.logic.M2IndexManager
import modules.m3.Invoice
import modules.m3.gui.InvoiceConfiguratorWizard
import modules.m3.gui.InvoiceViewerWizard
import modules.m3.misc.InvoiceProperty
import modules.m3.misc.getInvoiceFromInvoiceProperty
import modules.m3.misc.getInvoicePropertyFromInvoice
import tornadofx.Controller
import tornadofx.Scope

@ExperimentalSerializationApi
class M3Controller : IModule, Controller()
{
    override fun moduleNameLong() = "M3Controller"
    override fun module() = "M3"

    private val wizard = find<InvoiceConfiguratorWizard>()
    val db: CwODB by inject()

    fun openWizardNewInvoice(indexManager: M3IndexManager)
    {
        wizard.invoice.item = InvoiceProperty()
        wizard.isComplete = false
        wizard.onComplete {
            if (wizard.invoice.seller.value !== null)
            {
                val raf = db.openRandomFileAccess(module(), "rw")
                M3DBManager().saveEntry(
                    getInvoiceFromInvoiceProperty(wizard.invoice.item), db, -1L, -1, raf, indexManager
                )
                db.closeRandomFileAccess(raf)
                wizard.invoice.item = InvoiceProperty()
                wizard.isComplete = false
                wizard.close()
            }
        }
        wizard.openModal()
    }

    fun showInvoice(invoice: Invoice, indexManager: M3IndexManager, m2IndexManager: M2IndexManager)
    {
        val wizard = tornadofx.find<InvoiceViewerWizard>(Scope(m2IndexManager))
        wizard.invoice.item = getInvoicePropertyFromInvoice(invoice)
        wizard.onComplete {
            if (wizard.invoice.uID.value != -1)
            {
                val raf = db.openRandomFileAccess(module(), "rw")
                M3DBManager().saveEntry(
                    entry = getInvoiceFromInvoiceProperty(wizard.invoice.item),
                    cwodb = db,
                    posDB = indexManager.indexList[0]!!.indexMap[wizard.invoice.item.uID]!!.pos,
                    byteSize = indexManager.indexList[0]!!.indexMap[wizard.invoice.item.uID]!!.byteSize,
                    raf = raf,
                    indexManager = indexManager
                )
                this.db.closeRandomFileAccess(raf)
                wizard.invoice.item = InvoiceProperty()
                wizard.isComplete = false
                wizard.close()
            }
        }
        wizard.openModal()
    }
}