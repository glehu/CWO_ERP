package modules.mx.misc

import kotlinx.serialization.Serializable

@Serializable
data class EMailerIni(
  var defaultFooter: String = "", var writeStatistics: Boolean = true
)
