package modules.m3

import interfaces.IEntry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import modules.mx.activeUser

@ExperimentalSerializationApi
@Serializable
data class M3Item(
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
        //if (uID == -1) uID = m3GlobalIndex.getUID().toInt()
        if (userName.isEmpty()) userName = activeUser.username
    }
}