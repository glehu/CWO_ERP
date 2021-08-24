package api.misc.json

import kotlinx.serialization.Serializable

@Serializable
data class M1EntryListJson(
    var resultsAmount: Int,
    val resultsList: ArrayList<ByteArray>
)