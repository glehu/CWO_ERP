package modules.m2

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import modules.IEntry
import modules.mx.m2GlobalIndex

@Serializable
data class Contact(override var uID: Int, var name: String) : IEntry
{
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

    @ExperimentalSerializationApi
    fun initialize()
    {
        if (uID == -1) uID = m2GlobalIndex.getUID()
    }
}