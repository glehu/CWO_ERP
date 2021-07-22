package modules.m2.logic

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import modules.IModule
import modules.m1.misc.SongModel
import modules.m2.Contact
import modules.m2.gui.ContactConfiguratorWizard
import modules.m2.gui.MG2ContactFinder
import modules.m2.gui.MG2Analytics
import modules.m2.gui.MG2Import
import modules.m2.misc.ContactProperty
import modules.m2.misc.getContactFromProperty
import modules.m3.logic.M3DBManager
import tornadofx.Controller
import tornadofx.Scope
import tornadofx.find

@ExperimentalSerializationApi
class M2Controller : IModule, Controller()
{
    override fun moduleNameLong() = "M2Controller"
    override fun module() = "M2"

    private val wizard = find<ContactConfiguratorWizard>()
    val db: CwODB by inject()
    val indexManager: M2IndexManager by inject(Scope(db))

    fun openWizardNewContact()
    {
        wizard.contact.item = ContactProperty()
        wizard.isComplete = false
        wizard.onComplete {
            if (wizard.contact.name.value !== null)
            {
                val raf = db.openRandomFileAccess(module(), "rw")
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
        find(MG2ContactFinder::class, Scope(indexManager)).openModal()
    }

    fun openAnalytics()
    {
        //TODO: Add multiple analytics modes
        find(MG2Analytics::class, Scope(indexManager)).openModal()
    }

    fun openDataImport()
    {
        find(MG2Import::class, Scope(indexManager)).openModal()
    }

    fun selectContact(): Contact
    {
        val contact: Contact
        val newScope = Scope()
        val dataTransfer = SongModel()
        dataTransfer.uID.value = -2
        setInScope(dataTransfer, newScope)
        setInScope(indexManager, newScope)
        find<MG2ContactFinder>(newScope).openModal(block = true)
        contact = if (dataTransfer.name.value != null)
        {
            M2DBManager().getEntry(
                dataTransfer.uID.value, db, indexManager.indexList[0]!!
            ) as Contact
        } else Contact(-1, "")
        return contact
    }

    fun getContactName(uID: Int, default: String): String
    {
        return if (uID != -1)
        {
            val contact = M2DBManager().getEntry(
                uID, db, indexManager.indexList[0]!!
            ) as Contact
            contact.name
        } else default
    }
}