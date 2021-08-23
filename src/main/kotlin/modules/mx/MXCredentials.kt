package modules.mx

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import modules.mx.logic.MXUserManager

@Serializable
data class MXCredentials(val type: MXUserManager.CredentialsType) {
    @SerialName("c")
    val credentials = mutableMapOf<String, MXUser>()
}