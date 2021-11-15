package modules.m2.logic

import api.logic.getUserClient
import interfaces.IController
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.misc.SongPropertyMainDataModel
import modules.m2.M2Contact
import modules.m2.gui.ContactConfiguratorWizard
import modules.m2.gui.MG2Analytics
import modules.m2.gui.MG2ContactFinder
import modules.m2.gui.MG2Import
import modules.m2.misc.ContactProperty
import modules.m2.misc.getContactFromProperty
import modules.m2.misc.getContactPropertyFromContact
import modules.mx.activeUser
import modules.mx.gui.MGXEMailer
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
        return m2GlobalIndex!!
    }

    private val wizard = find<ContactConfiguratorWizard>()
    val client = getUserClient(activeUser.username, activeUser.password)

    override fun searchEntry() {
        find<MG2ContactFinder>().openModal()
    }

    override suspend fun saveEntry(unlock: Boolean) {
        if (wizard.contact.isValid) {
            wizard.contact.commit()
            wizard.contact.uID.value = save(getContactFromProperty(wizard.contact.item), unlock = unlock)
            wizard.isComplete = false
        }
    }

    override fun newEntry() {
        wizard.contact.commit()
        if (wizard.contact.isValid && wizard.contact.uID.value != -1) {
            setEntryLock(wizard.contact.uID.value, false)
        }
        wizard.contact.item = ContactProperty()
        wizard.contact.validate()
        wizard.isComplete = false
    }

    override fun showEntry(uID: Int) {
        val entry = get(uID) as M2Contact
        wizard.contact.item = getContactPropertyFromContact(entry)
    }

    fun openAnalytics() {
        //TODO: Add multiple analytics modes
        find<MG2Analytics>().openModal()
    }

    fun openDataImport() {
        find<MG2Import>().openModal()
    }

    fun selectAndLoadContact(): M2Contact {
        val contact: M2Contact
        val newScope = Scope()
        val dataTransfer = SongPropertyMainDataModel()
        dataTransfer.uID.value = -2
        setInScope(dataTransfer, newScope)
        find<MG2ContactFinder>(newScope).openModal(block = true)
        contact = if (dataTransfer.name.value != null) {
            load(dataTransfer.uID.value) as M2Contact
        } else M2Contact(-1, "")
        return contact
    }

    fun getContactName(uID: Int, default: String): String {
        return if (uID != -1) {
            (load(uID) as M2Contact).name
        } else default
    }

    fun showEMailer() {
        val mailer = find<MGXEMailer>()
        mailer.recipientProperty.value =
            if (wizard.contact.email.value.isNotEmpty() && wizard.contact.email.value != "?") {
                wizard.contact.email.value
            } else ""
        mailer.salutationProperty.value =
            if (wizard.contact.salutation.value.isNotEmpty() && wizard.contact.salutation.value != "?") {
                wizard.contact.salutation.value
            } else ""
        val contact = if (wizard.contact.uID.value != -1) {
            load(wizard.contact.uID.value) as M2Contact
        } else null
        if (contact != null) mailer.contact = contact
        mailer.openModal()
    }
}
