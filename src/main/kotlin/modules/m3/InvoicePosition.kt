package modules.m3

import interfaces.IEntry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@ExperimentalSerializationApi
@Serializable
data class InvoicePosition(
  @SerialName("i") override var uID: Long,

  @SerialName("d") var description: String
) : IEntry {
  @SerialName("gp")
  var grossPrice: Double = 0.0

  @SerialName("np")
  var netPrice: Double = 0.0

  @SerialName("n")
  var amount: Double = 0.0

  /*
  #### Storages & Stock Posting [1/3] ####
   */

  @SerialName("n1")
  var storageAmount1: Double = 0.0

  @SerialName("sf1")
  var storageFrom1UID: Int = -1

  @SerialName("suf1")
  var storageUnitFrom1UID: Int = -1

  @SerialName("spf1")
  var stockPostingsFrom1UID: MutableMap<Int, Double> = mutableMapOf()

  /*
  #### Storages & Stock Posting [2/3] ####
   */

  @SerialName("n2")
  var storageAmount2: Double = 0.0

  @SerialName("sf2")
  var storageFrom2UID: Int = -1

  @SerialName("suf2")
  var storageUnitFrom2UID: Int = -1

  @SerialName("spf2")
  var stockPostingsFrom2UID: MutableMap<Int, Double> = mutableMapOf()

  /*
  #### Storages & Stock Posting [3/3] ####
   */

  @SerialName("n3")
  var storageAmount3: Double = 0.0

  @SerialName("sf3")
  var storageFrom3UID: Int = -1

  @SerialName("suf3")
  var storageUnitFrom3UID: Int = -1

  @SerialName("spf3")
  var stockPostingsFrom3UID: MutableMap<Int, Double> = mutableMapOf()

  override fun initialize() {
  }
}
