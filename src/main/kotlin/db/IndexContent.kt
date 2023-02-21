package db

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IndexContent(
  @SerialName("u") val uID: Int = -1,
  @SerialName("c") var content: String = "",
  @SerialName("p") var pos: Long = -1L,
  @SerialName("b") var byteSize: Int = -1
)
