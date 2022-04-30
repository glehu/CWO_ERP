package modules.m5

import kotlinx.serialization.SerialName
import modules.mx.logic.Timestamp

@kotlinx.serialization.Serializable
data class UniMessage(
  @SerialName("src")
  val from: String,
  @SerialName("msg")
  val message: String,
  @SerialName("ts")
  val timestamp: String = Timestamp.now()
)
