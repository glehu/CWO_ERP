package modules.mx

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import modules.mx.logic.UserCLIManager

@ExperimentalSerializationApi
@InternalAPI
@Serializable
data class Credentials(val type: UserCLIManager.CredentialsType) {
  @SerialName("c")
  val credentials = mutableMapOf<String, User>()
}
