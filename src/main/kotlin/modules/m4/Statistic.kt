package modules.m4

import kotlinx.serialization.Serializable

@Serializable
data class Statistic(
  var description: String,
  var sValue: String,
  var nValue: Float,
  var number: Boolean
)
