package modules.m3

import interfaces.IEntry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@ExperimentalSerializationApi
@Serializable
data class M3InvoicePosition(
    @SerialName("i")
    override var uID: Int,
    @SerialName("d")
    var description: String
) : IEntry {
    @SerialName("p")
    var price: Double = 0.0

    @SerialName("a")
    var amount: Int = 1

    @SerialName("u")
    var userName: String = ""

    override fun initialize() {
    }
}