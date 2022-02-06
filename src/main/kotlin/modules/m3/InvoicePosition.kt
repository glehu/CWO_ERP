package modules.m3

import interfaces.IEntry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@ExperimentalSerializationApi
@Serializable
data class InvoicePosition(
  @SerialName("i")
  override var uID: Int,

  @SerialName("d")
  var description: String
) : IEntry {
  @SerialName("gp")
  var grossPrice: Double = 0.0

  @SerialName("np")
  var netPrice: Double = 0.0

  @SerialName("n")
  var amount: Double = 0.0

  /*
  #### Storages & Stock Posting [1/2] ####
   */

  @SerialName("sf1")
  var storageFrom1UID: Int = -1

  @SerialName("suf1")
  var storageUnitFrom1UID: Int = -1

  @SerialName("spf1")
  var stockPostingFrom1UID: Int = -1

  /*
  #### Storages & Stock Posting [2/2] ####
   */

  @SerialName("sf2")
  var storageFrom2UID: Int = -1

  @SerialName("suf2")
  var storageUnitFrom2UID: Int = -1

  @SerialName("spf2")
  var stockPostingFrom2UID: Int = -1

  override fun initialize() {
  }
}
