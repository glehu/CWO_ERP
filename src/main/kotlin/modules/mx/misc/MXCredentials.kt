package modules.mx.misc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import modules.mx.logic.MXPasswordManager

@Serializable
data class MXCredentials(val type: MXPasswordManager.CredentialsType)
{
    @SerialName("c") val credentials = mutableMapOf<String, MXUser>()
}