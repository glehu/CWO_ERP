package modules.m2.gui

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.misc.ContactModel
import modules.m3.gui.MG3InvoiceFinder
import modules.mx.m3GlobalIndex
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
            field("Sales") {
                textfield(contact.moneySent) {
                    contextmenu {
                        item("Show invoices").action {
                            val m3Finder = MG3InvoiceFinder()
                            m3Finder.exactSearch.isSelected = true
                            m3Finder.searchText.text = contact.name.value
                            m3Finder.ixNr.value = m3GlobalIndex.getIndexUserSelection()[1]
                            m3Finder.openModal()
                        }
                    }
                }.isEditable = false
            }
            field("Expenses") {
                textfield(contact.moneyReceived) {
                    contextmenu {
                        item("Show invoices").action {
                            val m3Finder = MG3InvoiceFinder()
                            m3Finder.exactSearch.isSelected = true
                            m3Finder.searchText.text = contact.name.value
                            m3Finder.ixNr.value = m3GlobalIndex.getIndexUserSelection()[0]
                            m3Finder.openModal()
                        }
                    }
                }.isEditable = false
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