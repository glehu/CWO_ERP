package modules.mx

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import modules.mx.logic.MXUserManager

@ExperimentalSerializationApi
@InternalAPI
@Serializable
data class MXCredentials(val type: MXUserManager.CredentialsType) {
    @SerialName("c")
    val credentials = mutableMapOf<String, MXUser>()
}