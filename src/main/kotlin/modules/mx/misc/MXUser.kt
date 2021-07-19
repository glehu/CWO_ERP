package modules.mx.misc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MXUser(@SerialName("u") var username: String, @SerialName("p") var password: String)
{
    var canAccessM1Song: Boolean = true
    var canAccessM2Contact: Boolean = true
}