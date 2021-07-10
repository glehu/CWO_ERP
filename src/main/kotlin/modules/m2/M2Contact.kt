package modules.m2

import db.CwODB
import kotlinx.serialization.Serializable
import modules.m2.misc.ContactProperty
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
data class Contact(var uID: Int, var name: String)
{
    /*  M2CONTACTS DB Version 0.1.0-PreAlpha

            Changelog:
                19.06.2021 V0.1.0-PreAlpha
                    Module created
    */
    //*************************************************
    //********************** User Input Data **********
    //*************************************************

    //----------------------------------v
    //--------- Personal Data ----------|
    //----------------------------------^
    var firstName: String = "?"
    var lastName: String = "?"
    var birthdate: String = "??.??.????"

    //----------------------------------v
    //--------- Location Data ----------|
    //----------------------------------^
    var street: String = "?"
    var houseNr: String = "?"
    var city: String = "?"
    var postCode: String = "?"
    var country: String = "?"

    //----------------------------------v
    //-------- Profession Data ---------|
    //----------------------------------^
    var isVocalist: Boolean = false
    var isProducer: Boolean = false
    var isInstrumentalist: Boolean = false
    var isManager: Boolean = false
    var isFan: Boolean = false

    fun initialize()
    {
        if (uID == -1)
        {
            val indexer = CwODB()
            uID = indexer.getUniqueID("M2")
        }
    }
}

fun getContactPropertyFromContact(contact: Contact): ContactProperty
{
    val contactProperty = ContactProperty()
    //For contactModel to be serialized, it has to be inserted into contact
    //---------------------------------v
    //----------- Main Data -----------|
    //---------------------------------^
    contactProperty.uniqueID = contact.uID
    contactProperty.name = contact.name
    //----------------------------------v
    //--------- Personal Data ----------|
    //----------------------------------^
    contactProperty.firstName = contact.firstName
    contactProperty.lastName = contact.lastName
    contactProperty.birthdate = LocalDate.parse(contact.birthdate, DateTimeFormatter.ofPattern("dd.MM.yyyy"))

    //----------------------------------v
    //--------- Location Data ----------|
    //----------------------------------^
    contactProperty.street = contact.street
    contactProperty.houseNr = contact.houseNr
    contactProperty.city = contact.city
    contactProperty.postCode = contact.postCode
    contactProperty.country = contact.country

    //----------------------------------v
    //-------- Profession Data ---------|
    //----------------------------------^
    contactProperty.isVocalist = contact.isVocalist
    contactProperty.isProducer = contact.isProducer
    contactProperty.isInstrumentalist = contact.isInstrumentalist
    contactProperty.isManager = contact.isManager
    contactProperty.isFan = contact.isFan

    return contactProperty
}

fun getContactFromProperty(contactProperty: ContactProperty): Contact
{
    val contact = Contact(-1, contactProperty.name)
    //For songModel to be serialized, it has to be inserted into song
    //---------------------------------v
    //----------- Main Data -----------|
    //---------------------------------^
    contact.uID = contactProperty.uniqueID
    //----------------------------------v
    //--------- Personal Data ----------|
    //----------------------------------^
    contact.firstName = contactProperty.firstName
    contact.lastName = contactProperty.lastName
    contact.birthdate = contactProperty.birthdate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

    //----------------------------------v
    //--------- Location Data ----------|
    //----------------------------------^
    contact.street = contactProperty.street
    contact.houseNr = contactProperty.houseNr
    contact.city = contactProperty.city
    contact.postCode = contactProperty.postCode
    contact.country = contactProperty.country

    //----------------------------------v
    //-------- Profession Data ---------|
    //----------------------------------^
    contact.isVocalist = contactProperty.isVocalist
    contact.isProducer = contactProperty.isProducer
    contact.isInstrumentalist = contactProperty.isInstrumentalist
    contact.isManager = contactProperty.isManager
    contact.isFan = contactProperty.isFan

    return contact
}