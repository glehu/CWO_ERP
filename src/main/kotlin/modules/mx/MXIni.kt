package modules.mx

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MXIni(
    @SerialName("Encryption Key") var token: String,
    @SerialName("Data Path") var dataPath: String,
    @SerialName("Max Search Results") var maxSearchResults: Int,
    @SerialName("Difference from UTC in Hours") var differenceFromUTC: Int,
    @SerialName("Client") var isClient: Boolean,
    @SerialName("Server IP Address") var serverIPAddress: String,
    @SerialName("SMTP Username") var emailUsername: String,
    @SerialName("SMTP Password") var emailPassword: String,
    @SerialName("SMTP Host") var emailHost: String,
    @SerialName("SMTP Port") var emailPort: String,
    @SerialName("Send EMails as") var emailAddress: String
)
