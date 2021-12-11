package modules.mx.misc

import kotlinx.serialization.Serializable

@Serializable
data class MGXEMailerIni(
    var defaultFooter: String = "",
    var writeStatistics: Boolean = true
)
