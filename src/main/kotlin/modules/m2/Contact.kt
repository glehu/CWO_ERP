package modules.m2

import api.misc.json.CWOAuthCallbackJson
import interfaces.IEntry
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import modules.mx.contactIndexManager

@InternalAPI
@ExperimentalSerializationApi
@Serializable
data class Contact(override var uID: Int, var name: String) : IEntry {
  @SerialName("u")
  var username: String = ""

  @SerialName("p")
  var password: String = ""

  //Rights
  @SerialName("rMX")
  var canAccessManagement: Boolean = false

  @SerialName("rM1")
  var canAccessDiscography: Boolean = true

  @SerialName("rM2")
  var canAccessContacts: Boolean = true

  @SerialName("rM3")
  var canAccessInvoices: Boolean = true

  @SerialName("rM4")
  var canAccessInventory: Boolean = true

  @SerialName("rM5")
  var canAccessClarifier: Boolean = true

  @SerialName("rM6")
  var canAccessSnippetBase: Boolean = true

  @SerialName("bt")
  var apiToken: CWOAuthCallbackJson = CWOAuthCallbackJson()

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
  var isDeveloper: Boolean = false

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
