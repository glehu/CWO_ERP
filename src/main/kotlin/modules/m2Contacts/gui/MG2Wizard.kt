package modules.m2Contacts.gui

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2Contacts.misc.ContactModel
import modules.m3Invoices.gui.MG3InvoiceFinder
import modules.m3Invoices.logic.M3Controller
import tornadofx.*

@ExperimentalSerializationApi
@InternalAPI
class ContactConfiguratorWizard : Wizard("Add new contact") {
    val contact: ContactModel by inject()

    init {
        enableStepLinks = true
        showHeader = false
        add(NewContactMainData::class)
        add(NewContactFinancialData::class)
        add(NewContactProfessionData::class)
        add(NewContactMiscData::class)
    }
}

class NewContactMainData : Fragment("Main Data") {
    private val contact: ContactModel by inject()

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    override val root = form {
        fieldset {
            field("UID") {
                label(contact.uID)
            }
            field("Name") { textfield(contact.name).required() }
            field("First Name") { textfield(contact.firstName) }
            field("Last Name") { textfield(contact.lastName) }
            field("Birthdate") { datepicker(contact.birthdate) }
            field("Street") { textfield(contact.street) }
            field("Number") { textfield(contact.houseNr) }
            field("City") { textfield(contact.city) }
            field("Postcode") { textfield(contact.postCode) }
            field("Country") { textfield(contact.country) }
        }
    }

    override fun onSave() {
        isComplete = contact.commit()
    }
}

@ExperimentalSerializationApi
@InternalAPI
class NewContactFinancialData : Fragment("Financial Data") {
    private val contact: ContactModel by inject()

    //----------------------------------v
    //-------- Financial Data ---------|
    //----------------------------------^
    override val root = form {
        fieldset {
            field("Price Category") { textfield(contact.priceCategory) }
            field("Contact's Sales") {
                hbox {
                    textfield(contact.moneyReceived) {
                        contextmenu {
                            item("Show invoices (as seller)").action {
                                val m3Finder = MG3InvoiceFinder()
                                m3Finder.exactSearch.isSelected = true
                                m3Finder.ixNr.value = M3Controller().getIndexUserSelection()[0]
                                m3Finder.openModal()
                                m3Finder.searchText.text = ""
                                m3Finder.searchText.text = contact.name.value
                            }
                        }
                    }.isEditable = false
                    label("EUR") { paddingHorizontal = 20 }
                }
            }
            field("Contact's Expenses") {
                hbox {
                    textfield(contact.moneySent) {
                        contextmenu {
                            item("Show invoices (as buyer)").action {
                                val m3Finder = MG3InvoiceFinder()
                                m3Finder.exactSearch.isSelected = true
                                m3Finder.ixNr.value = M3Controller().getIndexUserSelection()[1]
                                m3Finder.openModal()
                                m3Finder.searchText.text = ""
                                m3Finder.searchText.text = contact.name.value
                            }
                        }
                    }.isEditable = false
                    label("EUR") { paddingHorizontal = 20 }
                }
            }
        }
    }

    override fun onSave() {
        isComplete = contact.commit()
    }
}

class NewContactProfessionData : Fragment("Profession Data") {
    private val contact: ContactModel by inject()

    //----------------------------------v
    //-------- Profession Data ---------|
    //----------------------------------^
    override val root = form {
        fieldset {
            field("Vocalist") { checkbox("", contact.isVocalist) }
            field("Producer") { checkbox("", contact.isProducer) }
            field("Instrumentalist") { checkbox("", contact.isInstrumentalist) }
            field("Manager") { checkbox("", contact.isManager) }
            field("Fan") { checkbox("", contact.isFan) }
        }
    }

    override fun onSave() {
        isComplete = contact.commit()
    }
}

class NewContactMiscData : Fragment("Misc Data") {
    private val contact: ContactModel by inject()

    //----------------------------------v
    //----------- Misc Data ------------|
    //----------------------------------^
    override val root = form {
        fieldset {
            field("Spotify ID") { textfield(contact.spotifyID) }
        }
    }

    override fun onSave() {
        isComplete = contact.commit()
    }
}
