@file:Suppress(
  "DuplicatedCode", "DuplicatedCode", "DuplicatedCode", "DuplicatedCode", "DuplicatedCode",
  "DuplicatedCode", "DuplicatedCode", "DuplicatedCode", "DuplicatedCode", "DuplicatedCode", "DuplicatedCode",
  "DuplicatedCode", "DuplicatedCode", "DuplicatedCode", "DuplicatedCode"
)

package modules.m2.misc

import io.ktor.util.*
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m2.Contact
import modules.mx.Statistic
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.observableListOf
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
  val salutationProperty = SimpleStringProperty("?")
  var salutation: String by salutationProperty
  val firstNameProperty = SimpleStringProperty("?")
  var firstName: String by firstNameProperty
  val lastNameProperty = SimpleStringProperty("?")
  var lastName: String by lastNameProperty
  val birthdateProperty = SimpleObjectProperty(LocalDate.now())
  var birthdate: LocalDate by birthdateProperty
  val emailProperty = SimpleStringProperty("?")
  var email: String by emailProperty

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
  //-------- Statistics Data ---------|
  //----------------------------------^
  var statisticsProperty = observableListOf<Statistic>()

  //----------------------------------v
  //------------ API Data ------------|
  //----------------------------------^
  val spotifyIDProperty = SimpleStringProperty("?")
  var spotifyID: String by spotifyIDProperty
}

class ContactModel : ItemViewModel<ContactProperty>(ContactProperty()) {
  val uID = bind(ContactProperty::uIDProperty)
  val name = bind(ContactProperty::nameProperty)
  val salutation = bind(ContactProperty::salutationProperty)
  val firstName = bind(ContactProperty::firstNameProperty)
  val lastName = bind(ContactProperty::lastNameProperty)
  val birthdate = bind(ContactProperty::birthdateProperty)
  val email = bind(ContactProperty::emailProperty)
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
  val statistics = bind(ContactProperty::statisticsProperty)
  val spotifyID = bind(ContactProperty::spotifyIDProperty)
  val priceCategory = bind(ContactProperty::priceCategoryProperty)
  val moneySent = bind(ContactProperty::moneySentProperty)
  val moneyReceived = bind(ContactProperty::moneyReceivedProperty)
}

@InternalAPI
@ExperimentalSerializationApi
fun getContactPropertyFromContact(contact: Contact): ContactProperty {
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
  contactProperty.salutation = contact.salutation
  contactProperty.firstName = contact.firstName
  contactProperty.lastName = contact.lastName
  if (contact.birthdate != "??.??.????") {
    contactProperty.birthdate = LocalDate.parse(contact.birthdate, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
  }
  contactProperty.email = contact.email

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

  /**
   * Fill the item's statistics
   */
  for ((_, statisticString) in contact.statistics) {
    val statistic = Json.decodeFromString<Statistic>(statisticString)
    contactProperty.statisticsProperty.add(statistic)
  }

  //----------------------------------v
  //------------ API Data ------------|
  //----------------------------------^
  contactProperty.spotifyID = contact.spotifyID

  return contactProperty
}

@InternalAPI
@ExperimentalSerializationApi
fun getContactFromProperty(contactProperty: ContactProperty): Contact {
  val contact = Contact(-1, contactProperty.name)
  //For contactModel to be serialized, it has to be inserted into contact
  //---------------------------------v
  //----------- Main Data -----------|
  //---------------------------------^
  contact.uID = contactProperty.uID
  //----------------------------------v
  //--------- Personal Data ----------|
  //----------------------------------^
  contact.salutation = contactProperty.salutation
  contact.firstName = contactProperty.firstName
  contact.lastName = contactProperty.lastName
  contact.birthdate = contactProperty.birthdate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
  contact.email = contactProperty.email

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

  for (statistic in contactProperty.statisticsProperty) {
    contact.statistics[statistic.description] = Json.encodeToString(statistic)
  }

  //----------------------------------v
  //------------ API Data ------------|
  //----------------------------------^
  contact.spotifyID = contactProperty.spotifyID

  return contact
}
