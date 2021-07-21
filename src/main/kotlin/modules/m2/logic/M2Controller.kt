package modules.m2.logic

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import modules.IModule
import modules.m2.gui.ContactConfiguratorWizard
import modules.m2.gui.ContactFinder
import modules.m2.gui.MG2Analytics
import modules.m2.gui.MG2Import
import modules.m2.misc.ContactProperty
import modules.m2.misc.getContactFromProperty
import tornadofx.Controller
import tornadofx.Scope

@ExperimentalSerializationApi
class M2Controller : IModule, Controller()
{
    override fun moduleName() = "MG2UserInterface"

    private val wizard = find<ContactConfiguratorWizard>()
    val db: CwODB by inject()
    val indexManager: M2IndexManager by inject(Scope(db))

    fun openWizardNewContact()
    {
        wizard.contact.item = ContactProperty()
        wizard.isComplete = false
        wizard.onComplete {
            if (wizard.contact.item !== null)
            {
                val raf = db.openRandomFileAccess("M2", "rw")
                M2DBManager().saveEntry(
                    getContactFromProperty(wizard.contact.item), db, -1L, -1, raf, indexManager
                )
                db.closeRandomFileAccess(raf)
                wizard.contact.item = ContactProperty()
                wizard.isComplete = false
                wizard.close()
            }
        }
        wizard.openModal()
    }

    @ExperimentalSerializationApi
    fun openWizardFindContact()
    {
        tornadofx.find(ContactFinder::class, Scope(indexManager)).openModal()
    }

    fun openAnalytics()
    {
        //TODO: Add multiple analytics modes
        tornadofx.find(MG2Analytics::class, Scope(indexManager)).openModal()
    }

    fun openDataImport()
    {
        tornadofx.find(MG2Import::class, Scope(indexManager)).openModal()
    }
}