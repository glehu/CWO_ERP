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