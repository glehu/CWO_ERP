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

  override fun initialize() {
  }
}
