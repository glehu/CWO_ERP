package modules.mx

import kotlinx.serialization.Serializable

@Serializable
data class LastChange(
  var uID: Long,
  var unixHex: String,
  var user: String
)
