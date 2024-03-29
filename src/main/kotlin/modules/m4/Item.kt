package modules.m4

import interfaces.IEntry
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import modules.mx.itemIndexManager
import java.io.File
import java.util.*

@InternalAPI
@ExperimentalSerializationApi
@Serializable
data class Item(
  override var uID: Long,
  var description: String
) : IEntry {
  var articleNumber = ""
  var ean = ""
  var manufacturerCode = ""
  var imagePath = ""
  private var imageBase64String = ""

  /**
   * Various product info can be added to the item by providing a json string of its details.
   */
  var productInfoJson = ""

  //Webshop and Sales Statistics
  var statistics: MutableMap<String, String> = mutableMapOf()

  /**
   * This map contains the prices of this item for specific price categories.
   *
   * The key is the price category number. The value is the price.
   */
  var prices: MutableMap<Int, String> = mutableMapOf()

  /**
   * This map contains all stock distributed over all storage locations.
   */
  var stock: MutableMap<Int, String> = mutableMapOf()

  override fun initialize() {
    if (uID == -1L) uID = itemIndexManager!!.getUID()
    if (imagePath != "?") {
      imageBase64String = Base64.getEncoder().encodeToString(File(imagePath).readBytes())
    }
  }
}
