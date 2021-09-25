package modules.m2.misc

import javafx.beans.property.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.M2Contact
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.setValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ContactProperty {

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
    //--------- Financial Data ---------|
    //----------------------------------^
    val priceCategoryProperty = SimpleIntegerProperty(0)
    var priceCategory by priceCategoryProperty
    val moneySentProperty = SimpleDoubleProperty(0.0)
    var moneySent by moneySentProperty
    val moneyReceivedProperty = SimpleDoubleProperty(0.0)
    var moneyReceived by moneyReceivedProperty

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

    //----------------------------------v
    //------------ API Data ------------|
    //----------------------------------^
    val spotifyIDProperty = SimpleStringProperty("?")
    var spotifyID: String by spotifyIDProperty
}

class ContactModel : ItemViewModel<ContactProperty>(ContactProperty()) {
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
    val spotifyID = bind(ContactProperty::spotifyIDProperty)
    val priceCategory = bind(ContactProperty::priceCategoryProperty)
    val moneySent = bind(ContactProperty::moneySentProperty)
    val moneyReceived = bind(ContactProperty::moneyReceivedProperty)
}

@ExperimentalSerializationApi
fun getContactPropertyFromContact(contact: M2Contact): ContactProperty {
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
    //--------- Financial Data ---------|
    //----------------------------------^
    contactProperty.priceCategory = contact.priceCategory
    contactProperty.moneySent = contact.moneySent
    contactProperty.moneyReceived = contact.moneyReceived

    //----------------------------------v
    //-------- Profession Data ---------|
    //----------------------------------^
    contactProperty.isVocalist = contact.isVocalist
    contactProperty.isProducer = contact.isProducer
    contactProperty.isInstrumentalist = contact.isInstrumentalist
    contactProperty.isManager = contact.isManager
    contactProperty.isFan = contact.isFan

    //----------------------------------v
    //------------ API Data ------------|
    //----------------------------------^
    contactProperty.spotifyID = contact.spotifyID

    return contactProperty
}

@ExperimentalSerializationApi
fun getContactFromProperty(contactProperty: ContactProperty): M2Contact {
    val contact = M2Contact(-1, contactProperty.name)
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
    //--------- Financial Data ---------|
    //----------------------------------^
    contact.priceCategory = contactProperty.priceCategory
    contact.moneySent = contactProperty.moneySent
    contact.moneyReceived = contactProperty.moneyReceived

    //----------------------------------v
    //-------- Profession Data ---------|
    //----------------------------------^
    contact.isVocalist = contactProperty.isVocalist
    contact.isProducer = contactProperty.isProducer
    contact.isInstrumentalist = contactProperty.isInstrumentalist
    contact.isManager = contactProperty.isManager
    contact.isFan = contactProperty.isFan

    //----------------------------------v
    //------------ API Data ------------|
    //----------------------------------^
    contact.spotifyID = contactProperty.spotifyID

    return contact
}