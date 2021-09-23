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
    var articleNumber = ""
    var ean = ""
    var manufacturerCode = ""

    /**
     * Various product info can be added to the item by providing a json string of its details.
     */
    var productInfoJson = ""

    /**
     * This map contains the prices of this item for specific price categories.
     *
     * The key is the price category number. The value is the price.
     */
    var prices: MutableMap<Int, String> = mutableMapOf()

    override fun initialize() {
        if (uID == -1) uID = m4GlobalIndex.getUID().toInt()
    }
}