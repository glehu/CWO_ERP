package modules.mx

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MXCredentials(val type: MXPasswordManager.CredentialsType)
{
    @SerialName("c") val credentials = mutableMapOf<String, MXUser>()
}