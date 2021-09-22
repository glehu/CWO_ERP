package modules.m4

import interfaces.IEntry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import modules.mx.m4GlobalIndex

@ExperimentalSerializationApi
@Serializable
data class M4Item(
    override var uID: Int,
    var description: String
) : IEntry {

    override fun initialize() {
        if (uID == -1) uID = m4GlobalIndex.getUID().toInt()
    }
}