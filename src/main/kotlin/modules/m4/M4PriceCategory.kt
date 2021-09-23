package modules.m4

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class M4PriceCategory(
    @SerialName("n")
    val number: Int,
    @SerialName("d")
    val description: String
) {
    @SerialName("p")
    var price: Double = 0.0
}