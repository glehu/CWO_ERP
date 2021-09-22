package modules.mx

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MXUser(@SerialName("u") var username: String, @SerialName("p") var password: String) {
    var canAccessMX: Boolean = false
    var canAccessM1: Boolean = true
    var canAccessM2: Boolean = true
    var canAccessM3: Boolean = true
    var canAccessM4: Boolean = true
}