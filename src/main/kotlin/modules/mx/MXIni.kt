package modules.mx

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MXIni(
    @SerialName("encryption key") var token: String,
    @SerialName("data path") var dataPath: String,
    @SerialName("max search results") var maxSearchResults: Int,
    @SerialName("difference from utc in hours") var differenceFromUTC: Int,
    @SerialName("client") var isClient: Boolean,
    @SerialName("server ip address") var serverIPAddress: String,
    @SerialName("EMail Address") var emailUsername: String,
    @SerialName("SMTP Password") var emailPassword: String,
    @SerialName("SMTP Host") var emailHost: String,
    @SerialName("SMTP Port") var emailPort: String
)
