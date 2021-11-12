package modules.m4

import kotlinx.serialization.Serializable

@Serializable
data class M4Statistic(
    var description: String,
    var sValue: String,
    var nValue: Float,
    var number: Boolean
)
