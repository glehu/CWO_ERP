package modules.m4.misc

import kotlinx.serialization.Serializable

@Serializable
data class ItemIni(
  var rightsToAddStock: MutableMap<Int, String> = mutableMapOf(),
)
