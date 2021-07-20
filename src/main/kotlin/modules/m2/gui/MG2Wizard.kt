package modules.m2.gui

import modules.m2.misc.ContactModel
import tornadofx.*

class ContactConfiguratorWizard : Wizard("Add new contact")
{
    val contact: ContactModel by inject()

    init
    {
        enableStepLinks = true
        add(NewContactMainData::class)
    }
}

class ContactViewerWizard : Wizard("View a contact")
{
    val contact: ContactModel by inject()

    init
    {
        enableStepLinks = true
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
            field("Name") { textfield(contact.name).required() }
            field("First Name") { textfield(contact.firstName) }
            field("Last Name") { textfield(contact.lastName) }
            field("Birthdate") { datepicker(contact.birthdate) }
            field("Street") { textfield(contact.street) }
            field("City") { textfield(contact.city) }
            field("Postcode") { textfield(contact.postCode) }
            field("Country") { textfield(contact.country) }
            field("Vocalist") { checkbox("", contact.isVocalist) }
            field("Producer") { checkbox("", contact.isProducer) }
            field("Instrumentalist") { checkbox("", contact.isInstrumentalist) }
            field("Manager") { checkbox("", contact.isManager) }
            field("Fan") { checkbox("", contact.isFan) }
        }
    }

    override fun onSave()
    {
        isComplete = contact.commit()
    }
}