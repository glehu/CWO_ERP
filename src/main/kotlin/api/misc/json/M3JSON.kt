package api.misc.json

import kotlinx.serialization.Serializable

@Serializable
data class M3EntryListJson(
    var resultsAmount: Int,
    val resultsList: ArrayList<ByteArray>
)

@Serializable
data class M3EntryJson(
    var uID: Int,
    var entry: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as M1EntryJson
        if (!entry.contentEquals(other.entry)) return false
        return true
    }
    override fun hashCode(): Int {
        return entry.contentHashCode()
    }
}