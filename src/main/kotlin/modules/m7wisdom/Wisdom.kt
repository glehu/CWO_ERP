package modules.m7wisdom

import com.benasher44.uuid.Uuid
import interfaces.IEntry
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import modules.mx.logic.Timestamp
import modules.mx.wisdomIndexManager

@InternalAPI
@ExperimentalSerializationApi
@kotlinx.serialization.Serializable
data class Wisdom(
  override var uID: Int = -1
) : IEntry {
  @SerialName("t")
  var title: String = ""

  @SerialName("author")
  var authorUsername: String = ""

  @SerialName("desc")
  var description: String = ""
  var copyContent: String = ""
  var keywords: String = ""
  var type: String = ""
  var categories: ArrayList<String> = arrayListOf()

  var gUID: String = ""

  // References the Knowledge entry it belongs to
  var knowledgeUID: Int = -1

  // References another (sub) Wisdom entry (e.g. Wisdom that got commented)
  var refWisdomUID: Int = -1

  // References the source parent Wisdom entry
  var srcWisdomUID: Int = -1

  @SerialName("cdate")
  var dateCreated: String = ""

  @SerialName("reacts")
  var reactions: ArrayList<String> = arrayListOf()

  init {
    if (dateCreated.isEmpty()) dateCreated = Timestamp.getUnixTimestampHex()
    if (gUID.isEmpty()) gUID = Uuid.randomUUID().toString()
    if (type.isEmpty()) type = "info"
  }

  override fun initialize() {
    if (uID == -1) uID = wisdomIndexManager!!.getUID()
  }
}