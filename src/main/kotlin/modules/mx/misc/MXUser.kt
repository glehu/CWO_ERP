package modules.mx.misc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MXUser(@SerialName("u") var username: String, @SerialName("p") var password: String)