package modules.m4

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class M4StorageUnit(
    @SerialName("n")
    val number: Int,
    @SerialName("d")
    var description: String = "",
    @SerialName("l")
    var locked: Boolean = false
) {
    @SerialName("s")
    var stock: Double = 0.0
}
