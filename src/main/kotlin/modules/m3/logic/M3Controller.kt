package modules.m3.logic

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import modules.IModule
import modules.m3.gui.InvoiceConfiguratorWizard
import modules.m3.gui.MG3InvoiceFinder
import modules.m3.misc.InvoiceProperty
import modules.m3.misc.getInvoiceFromInvoiceProperty
import tornadofx.Controller
import tornadofx.Scope
import tornadofx.find

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
}