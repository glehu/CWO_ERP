package modules.m2

import com.benasher44.uuid.Uuid
import interfaces.IEntry
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import modules.mx.contactIndexManager

@InternalAPI
@ExperimentalSerializationApi
@Serializable
data class Contact(override var uID: Long, var name: String) : IEntry {
  var guid: String = ""

  //----------------------------------v
  //--------- Credentials ------------|
  //----------------------------------^
  @SerialName("u")
  var username: String = ""

  @SerialName("p")
  var password: String = ""

  //----------------------------------v
  //--------- Rights Data ------------|
  //----------------------------------^
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

  //----------------------------------v
  //--------- Personal Data ----------|
  //----------------------------------^
  var email: String = "?"

  //----------------------------------v
  //--------- Location Data ----------|
  //----------------------------------^
  var city: String = "?"

  //----------------------------------v
  //--------- Financial Data ---------|
  //----------------------------------^
  var moneySent: Double = 0.0

  //----------------------------------v
  //--------- Achievements -----------|
  //----------------------------------^
  var badges: ArrayList<String> = arrayListOf()

  override fun initialize() {
    if (uID == -1L) uID = contactIndexManager!!.getUID()
    if (guid.isEmpty()) guid = Uuid.randomUUID().toString()
  }
}
