package modules.m2.misc

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.setValue
import java.time.LocalDate

class ContactProperty
{

    val uIDProperty = SimpleIntegerProperty(-1)
    var uniqueID by uIDProperty
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
    val uniqueID = bind(ContactProperty::uIDProperty)
    val name = bind(ContactProperty::nameProperty)
    val firstName = bind(ContactProperty::firstNameProperty)
    val lastName = bind(ContactProperty::lastNameProperty)
    val birthdate = bind(ContactProperty::birthdateProperty)
    var street = bind(ContactProperty::streetProperty)
    var houseNr = bind(ContactProperty::houseNrProperty)
    var city = bind(ContactProperty::cityProperty)
    var postCode = bind(ContactProperty::postCodeProperty)
    var country = bind(ContactProperty::countryProperty)
    var isVocalist = bind(ContactProperty::isVocalistProperty)
    var isProducer = bind(ContactProperty::isProducerProperty)
    var isInstrumentalist = bind(ContactProperty::isInstrumentalistProperty)
    var isManager = bind(ContactProperty::isManagerProperty)
    var isFan = bind(ContactProperty::isFanProperty)
}