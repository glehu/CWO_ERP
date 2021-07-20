package db

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IndexContent(
    @SerialName("u") val uID: Int,
    @SerialName("c") val content: String,
    @SerialName("p") var pos: Long,
    @SerialName("b") var byteSize: Int
)