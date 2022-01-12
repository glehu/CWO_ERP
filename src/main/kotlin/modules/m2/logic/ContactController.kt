package modules.m2.logic

import interfaces.IController
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.misc.SongPropertyMainDataModel
import modules.m2.Contact
import modules.m2.gui.ContactConfiguratorWizard
import modules.m2.gui.GContactAnalytics
import modules.m2.gui.GContactFinder
import modules.m2.gui.GContactImport
import modules.m2.misc.ContactProperty
import modules.m2.misc.getContactFromProperty
import modules.m2.misc.getContactPropertyFromContact
import modules.mx.contactIndexManager
import modules.mx.gui.GEMailer
import tornadofx.Controller
import tornadofx.Scope
import tornadofx.find

@InternalAPI
@ExperimentalSerializationApi
class ContactController : IController, Controller() {
  override val moduleNameLong = "ContactController"
  override val module = "M2"
  override fun getIndexManager(): IIndexManager {
    return contactIndexManager!!
  }

  private val wizard = find<ContactConfiguratorWizard>()

  override fun searchEntry() {
    find<GContactFinder>().openModal()
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
    val entry = get(uID) as Contact
    wizard.contact.item = getContactPropertyFromContact(entry)
  }

  fun openAnalytics() {
    //TODO: Add multiple analytics modes
    find<GContactAnalytics>().openModal()
  }

  fun openDataImport() {
    find<GContactImport>().openModal()
  }

  fun selectAndLoadContact(): Contact {
    val contact: Contact
    val newScope = Scope()
    val dataTransfer = SongPropertyMainDataModel()
    dataTransfer.uID.value = -2
    setInScope(dataTransfer, newScope)
    find<GContactFinder>(newScope).openModal(block = true)
    contact = if (dataTransfer.name.value != null) {
      load(dataTransfer.uID.value) as Contact
    } else Contact(-1, "")
    return contact
  }

  fun getContactName(uID: Int, default: String): String {
    return if (uID != -1) {
      (load(uID) as Contact).name
    } else default
  }

  fun showEMailer() {
    val mailer = find<GEMailer>()
    mailer.recipientProperty.value =
      if (wizard.contact.email.value.isNotEmpty() && wizard.contact.email.value != "?") {
        wizard.contact.email.value
      } else ""
    mailer.salutationProperty.value =
      if (wizard.contact.salutation.value.isNotEmpty() && wizard.contact.salutation.value != "?") {
        wizard.contact.salutation.value
      } else ""
    val contact = if (wizard.contact.uID.value != -1) {
      load(wizard.contact.uID.value) as Contact
    } else null
    if (contact != null) mailer.contact = contact
    mailer.openModal()
  }
}
