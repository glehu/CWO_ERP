package modules.m2.misc

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import modules.m2.Contact
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.setValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ContactProperty
{

    val uIDProperty = SimpleIntegerProperty(-1)
    var uID: Int by uIDProperty
    val nameProperty = SimpleStringProperty()
    var name: String by nameProperty

    //----------------------------------v
    //--------- Personal Data ----------|
    //----------------------------------^
    val firstNameProperty = SimpleStringProperty("?")
    var firstName: String by firstNameProperty
    val lastNameProperty = SimpleStringProperty("?")
    var lastName: String by lastNameProperty
    val birthdateProperty = SimpleObjectProperty(LocalDate.now())
    var birthdate: LocalDate by birthdateProperty

    //----------------------------------v
    //--------- Location Data ----------|
    //----------------------------------^
    val streetProperty = SimpleStringProperty("?")
    var street: String by streetProperty
    val houseNrProperty = SimpleStringProperty("?")
    var houseNr: String by houseNrProperty
    val cityProperty = SimpleStringProperty("?")
    var city: String by cityProperty
    val postCodeProperty = SimpleStringProperty("?")
    var postCode: String by postCodeProperty
    val countryProperty = SimpleStringProperty("?")
    var country: String by countryProperty

    //----------------------------------v
    //-------- Profession Data ---------|
    //----------------------------------^
    val isVocalistProperty = SimpleBooleanProperty(false)
    var isVocalist by isVocalistProperty
    val isProducerProperty = SimpleBooleanProperty(false)
    var isProducer by isProducerProperty
    val isInstrumentalistProperty = SimpleBooleanProperty(false)
    var isInstrumentalist by isInstrumentalistProperty
    val isManagerProperty = SimpleBooleanProperty(false)
    var isManager by isManagerProperty
    val isFanProperty = SimpleBooleanProperty(false)
    var isFan by isFanProperty
}

class ContactModel : ItemViewModel<ContactProperty>(ContactProperty())
{
    val uID = bind(ContactProperty::uIDProperty)
    val name = bind(ContactProperty::nameProperty)
    val firstName = bind(ContactProperty::firstNameProperty)
    val lastName = bind(ContactProperty::lastNameProperty)
    val birthdate = bind(ContactProperty::birthdateProperty)
    val street = bind(ContactProperty::streetProperty)
    val houseNr = bind(ContactProperty::houseNrProperty)
    val city = bind(ContactProperty::cityProperty)
    val postCode = bind(ContactProperty::postCodeProperty)
    val country = bind(ContactProperty::countryProperty)
    val isVocalist = bind(ContactProperty::isVocalistProperty)
    val isProducer = bind(ContactProperty::isProducerProperty)
    val isInstrumentalist = bind(ContactProperty::isInstrumentalistProperty)
    val isManager = bind(ContactProperty::isManagerProperty)
    val isFan = bind(ContactProperty::isFanProperty)
}

fun getContactPropertyFromContact(contact: Contact): ContactProperty
{
    val contactProperty = ContactProperty()
    //For contactModel to be serialized, it has to be inserted into contact
    //---------------------------------v
    //----------- Main Data -----------|
    //---------------------------------^
    contactProperty.uID = contact.uID
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
    //For contactModel to be serialized, it has to be inserted into contact
    //---------------------------------v
    //----------- Main Data -----------|
    //---------------------------------^
    contact.uID = contactProperty.uID
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