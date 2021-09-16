package modules.m2.logic

import api.logic.getCWOClient
import interfaces.IController
import interfaces.IEntry
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.misc.SongPropertyMainDataModel
import modules.m2.Contact
import modules.m2.gui.ContactConfiguratorWizard
import modules.m2.gui.MG2Analytics
import modules.m2.gui.MG2ContactFinder
import modules.m2.gui.MG2Import
import modules.m2.misc.ContactProperty
import modules.m2.misc.getContactFromProperty
import modules.m2.misc.getContactPropertyFromContact
import modules.mx.activeUser
import modules.mx.m2GlobalIndex
import tornadofx.Controller
import tornadofx.Scope
import tornadofx.find

@InternalAPI
@ExperimentalSerializationApi
class M2Controller : IController, Controller() {
    override val moduleNameLong = "M2Controller"
    override val module = "M2"
    override fun getIndexManager(): IIndexManager {
        return m2GlobalIndex
    }

    private val wizard = find<ContactConfiguratorWizard>()
    val client = getCWOClient(activeUser.username, activeUser.password)

    override fun searchEntry() {
        find<MG2ContactFinder>().openModal()
    }

    override fun saveEntry() {
        if (wizard.contact.isValid) {
            wizard.contact.commit()
            wizard.contact.uID.value = save(getContactFromProperty(wizard.contact.item))
            wizard.isComplete = false
        }
    }

    override fun newEntry() {
        wizard.contact.item = ContactProperty()
        wizard.contact.validate()
        wizard.isComplete = false
    }

    override fun showEntry(entry: IEntry) {
        entry as Contact
        wizard.contact.item = getContactPropertyFromContact(entry)
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
            get(dataTransfer.uID.value) as Contact
        } else Contact(-1, "")
        return contact
    }

    fun getContactName(uID: Int, default: String): String {
        return if (uID != -1) {
            (get(uID) as Contact).name
        } else default
    }

    fun showEntry(uID: Int) {
        showEntry(get(uID) as Contact)
    }
}