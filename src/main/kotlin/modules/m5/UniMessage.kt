package modules.m5

import kotlinx.serialization.SerialName

@kotlinx.serialization.Serializable
data class UniMessage(
  @SerialName("src")
  val from: String,
  @SerialName("msg")
  val message: String,
  @SerialName("ts")
  val timestamp: String
)
