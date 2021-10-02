package modules.mx

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MXUser(
    @SerialName("u") var username: String,
    @SerialName("p") var password: String
) {
    //Rights
    @SerialName("rMX")
    var canAccessMX: Boolean = false

    @SerialName("rM1")
    var canAccessM1: Boolean = true

    @SerialName("rM2")
    var canAccessM2: Boolean = true

    @SerialName("rM3")
    var canAccessM3: Boolean = true

    @SerialName("rM4")
    var canAccessM4: Boolean = true

    //Misc
    @SerialName("ol")
    var online: Boolean = false

    @SerialName("ols")
    var onlineSince: String = "?"
}