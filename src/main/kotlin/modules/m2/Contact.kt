package modules.m2

import interfaces.IEntry
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import modules.mx.contactIndexManager

@InternalAPI
@ExperimentalSerializationApi
@Serializable
data class Contact(override var uID: Int, var name: String) : IEntry {
    //*************************************************
    //********************** User Input Data **********
    //*************************************************

    //----------------------------------v
    //--------- Personal Data ----------|
    //----------------------------------^
    var salutation: String = "?"
    var firstName: String = "?"
    var lastName: String = "?"
    var birthdate: String = "??.??.????"
    var email: String = "?"

    //----------------------------------v
    //--------- Location Data ----------|
    //----------------------------------^
    var street: String = "?"
    var houseNr: String = "?"
    var city: String = "?"
    var postCode: String = "?"
    var country: String = "?"

    //----------------------------------v
    //--------- Financial Data ---------|
    //----------------------------------^
    var priceCategory: Int = 0
    var moneySent: Double = 0.0
    var moneyReceived: Double = 0.0

    //----------------------------------v
    //-------- Profession Data ---------|
    //----------------------------------^
    var isVocalist: Boolean = false
    var isProducer: Boolean = false
    var isInstrumentalist: Boolean = false
    var isManager: Boolean = false
    var isFan: Boolean = false

    //----------------------------------v
    //-------- Statistics Data ---------|
    //----------------------------------^
    var statistics: MutableMap<String, String> = mutableMapOf()

    //----------------------------------v
    //------------ API Data ------------|
    //----------------------------------^
    var spotifyID: String = "?"

    override fun initialize() {
        if (uID == -1) uID = contactIndexManager!!.getUID()
    }
}