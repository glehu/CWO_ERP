package modules.mx

import io.ktor.util.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import modules.mx.logic.Log

@InternalAPI
@Serializable
data class LogMessage(
  @SerialName("ts") val unixTimestamp: String,
  @SerialName("md") val module: String,
  @SerialName("tp") val type: Log.Type,
  @SerialName("tx") val text: String,
  @SerialName("cl") val caller: String,
  @SerialName("ap") val apiEndpoint: String = ""
)
