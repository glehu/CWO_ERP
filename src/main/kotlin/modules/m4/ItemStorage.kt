package modules.m4

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemStorage(
  @SerialName("n")
  val number: Int,
  @SerialName("d")
  val description: String = "",
  @SerialName("l")
  var locked: Boolean = false
) {
  @SerialName("su")
  var storageUnits: MutableList<ItemStorageUnit> = mutableListOf()
}
