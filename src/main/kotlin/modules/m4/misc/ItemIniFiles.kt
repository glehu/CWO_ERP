package modules.m4.misc

import kotlinx.serialization.Serializable

@Serializable
data class M4Ini(
    var rightsToAddStock: MutableMap<Int, String> = mutableMapOf(),
)
