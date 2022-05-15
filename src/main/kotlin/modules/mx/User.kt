package modules.mx

import api.misc.json.CWOAuthCallbackJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
  @SerialName("u") var username: String,
  @SerialName("p") var password: String
) {
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

  @SerialName("ol")
  var online: Boolean = false

  @SerialName("bt")
  var apiToken: CWOAuthCallbackJson = CWOAuthCallbackJson()

  @SerialName("ols")
  var onlineSince: String = "?"
}
