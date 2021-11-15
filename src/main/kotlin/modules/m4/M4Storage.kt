package modules.m4

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class M4Storage(
    @SerialName("n")
    val number: Int,
    @SerialName("d")
    val description: String,
) {
    @SerialName("s")
    var stock: Int = 0
}
