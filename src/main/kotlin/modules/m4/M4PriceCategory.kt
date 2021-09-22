package modules.m4

import kotlinx.serialization.Serializable

@Serializable
data class M4PriceCategory(
    val number: Int,
    val description: String
)