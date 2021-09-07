package modules.m3

import interfaces.IEntry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import modules.mx.activeUser

@ExperimentalSerializationApi
@Serializable
data class M3Item(override var uID: Int, var description: String) : IEntry {
    var price: Double = 0.0
    var amount: Int = 1
    var userName: String = ""

    override fun initialize() {
        //if (uID == -1) uID = m3GlobalIndex.getUID().toInt()
        if (userName.isEmpty()) userName = activeUser.username
    }
}