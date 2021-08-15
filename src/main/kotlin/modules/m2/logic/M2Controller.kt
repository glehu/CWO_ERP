package modules.m2.logic

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import modules.IModule
import modules.m1.misc.SongModelP1
import modules.m2.Contact
import modules.m2.gui.*
import modules.m2.misc.ContactProperty
import modules.m2.misc.getContactFromProperty
import modules.m2.misc.getContactPropertyFromContact
import modules.mx.m2GlobalIndex
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

    fun openSearchScreen()
    {
        find<MG2ContactFinder>().openModal()
    }

    fun saveContact()
    {
        var isComplete = true
        wizard.contact.commit()
        if(!wizard.contact.isValid) isComplete = false
        if (isComplete)
        {
            val raf = db.openRandomFileAccess(module(), "rw")
            M2DBManager().saveEntry(
                getContactFromProperty(wizard.contact.item), db, -1L, -1, raf, m2GlobalIndex
            )
            db.closeRandomFileAccess(raf)
            wizard.isComplete = false
        }
    }

    fun openWizardNewContact()
    {
        wizard.contact.item = ContactProperty()
        wizard.isComplete = false
    }

    fun openAnalytics()
    {
        //TODO: Add multiple analytics modes
        find<MG2Analytics>().openModal()
    }

    fun openDataImport()
    {
        find<MG2Import>().openModal()
    }

    fun selectAndReturnContact(): Contact
    {
        val contact: Contact
        val newScope = Scope()
        val dataTransfer = SongModelP1()
        dataTransfer.uID.value = -2
        setInScope(dataTransfer, newScope)
        find<MG2ContactFinder>(newScope).openModal(block = true)
        contact = if (dataTransfer.name.value != null)
        {
            M2DBManager().getEntry(
                dataTransfer.uID.value, db, m2GlobalIndex.indexList[0]!!
            ) as Contact
        } else Contact(-1, "")
        return contact
    }

    fun getContactName(uID: Int, default: String): String
    {
        return if (uID != -1)
        {
            val contact = M2DBManager().getEntry(
                uID, db, m2GlobalIndex.indexList[0]!!
            ) as Contact
            contact.name
        } else default
    }

    fun showContact(uID: Int)
    {
        val contact = M2DBManager().getEntry(uID, db, m2GlobalIndex.indexList[0]!!) as Contact
        val wizard = find<ContactViewerWizard>()
        wizard.contact.item = getContactPropertyFromContact(contact)
        wizard.onComplete {
            if (wizard.contact.uID.value != -1)
            {
                val raf = db.openRandomFileAccess(module(), "rw")
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
                //wizard.close()
            }
        }
        //wizard.openWindow(block = true)
    }
}