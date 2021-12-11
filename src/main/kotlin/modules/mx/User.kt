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
    var canAccessMX: Boolean = false

    @SerialName("rM1")
    var canAccessM1: Boolean = true

    @SerialName("rM2")
    var canAccessM2: Boolean = true

    @SerialName("rM3")
    var canAccessM3: Boolean = true

    @SerialName("rM4")
    var canAccessM4: Boolean = true

    @SerialName("ol")
    var online: Boolean = false

    @SerialName("bt")
    var apiToken: CWOAuthCallbackJson = CWOAuthCallbackJson()

    @SerialName("ols")
    var onlineSince: String = "?"
}
