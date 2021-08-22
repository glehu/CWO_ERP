package modules.m2.gui

import modules.m2.misc.ContactModel
import tornadofx.*

class ContactConfiguratorWizard : Wizard("Add new contact")
{
    val contact: ContactModel by inject()

    init
    {
        enableStepLinks = true
        showHeader = false
        add(NewContactMainData::class)
    }
}

class NewContactMainData : Fragment("Main")
{
    private val contact: ContactModel by inject()

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    override val root = form {
        fieldset {
            field("UID") {
                textfield(contact.uID).isEditable = false
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
            field("Vocalist") { checkbox("", contact.isVocalist) }
            field("Producer") { checkbox("", contact.isProducer) }
            field("Instrumentalist") { checkbox("", contact.isInstrumentalist) }
            field("Manager") { checkbox("", contact.isManager) }
            field("Fan") { checkbox("", contact.isFan) }
            field("Spotify ID") { textfield(contact.spotifyID) }
        }
    }

    override fun onSave()
    {
        isComplete = contact.commit()
    }
}