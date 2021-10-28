package api.misc.json

import kotlinx.serialization.Serializable

@Serializable
data class EntryBytesListJson(
    var total: Int,
    val resultsList: ArrayList<ByteArray>
)

@Serializable
data class EntryListJson(
    var total: Int,
    val resultsList: ArrayList<String>
)

@Serializable
data class EntryJson(
    var uID: Int,
    var entry: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EntryJson
        if (!entry.contentEquals(other.entry)) return false
        return true
    }

    override fun hashCode(): Int {
        return entry.contentHashCode()
    }
}

@Serializable
data class LoginResponseJson(
    val httpCode: Int,
    val accessM1: Boolean,
    val accessM2: Boolean,
    val accessM3: Boolean,
    val accessM4: Boolean
)

/**
 * This validation container can be used to send or store data with its hash value.
 * A blockchain can be implemented using this container as the block.
 */
@Serializable
data class ValidationContainerJson(
    val contentJson: String,
    val hash: String
)

@Serializable
data class UPPriceCategoryJson(
    val catNew: String,
    val catOld: String
)

@Serializable
data class WebshopOrder(
    val itemUIDs: Array<Int>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as WebshopOrder
        if (!itemUIDs.contentEquals(other.itemUIDs)) return false
        return true
    }

    override fun hashCode(): Int {
        return itemUIDs.contentHashCode()
    }
}
