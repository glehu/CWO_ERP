package modules.m6

import com.benasher44.uuid.Uuid
import interfaces.IEntry
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import modules.mx.logic.Timestamp
import modules.mx.snippetBaseIndexManager

@InternalAPI
@ExperimentalSerializationApi
@kotlinx.serialization.Serializable
data class Snippet(
  override var uID: Long = -1L,
) : IEntry {
  override fun initialize() {
    if (uID == -1L) uID = snippetBaseIndexManager!!.getUID()
    if (guid.isEmpty()) guid = "snippet-" + Uuid.randomUUID().toString()
    if (dateCreated.isEmpty()) dateCreated = Timestamp.getUnixTimestampHex()
  }

  @SerialName("guid")
  var guid = ""

  @SerialName("cdate")
  var dateCreated = ""
  var payload = ""
  var payloadType = ""
  var payloadName = ""
  var payloadSizeMB = 0.0
  var payloadMimeType = ""
  var srcUniChatroomUID = -1L
  var srcWisdomUID = -1L
  var srcProcessUID = -1L
}
