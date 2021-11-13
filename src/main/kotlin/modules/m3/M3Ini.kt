package modules.m3

import kotlinx.serialization.Serializable

@Serializable
data class M3Ini(
    var autoCreateContacts: Boolean = true
)
