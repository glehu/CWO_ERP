package modules.m6

import com.benasher44.uuid.Uuid
import interfaces.IEntry
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import modules.mx.logic.Timestamp
import modules.mx.uniChatroomIndexManager

@InternalAPI
@ExperimentalSerializationApi
@kotlinx.serialization.Serializable
data class Snippet(
  override var uID: Long = -1,
) : IEntry {
  override fun initialize() {
    if (uID == -1L) uID = uniChatroomIndexManager!!.getUID()
    if (gUID.isEmpty()) gUID = "snippet-" + Uuid.randomUUID().toString()
    if (dateCreated.isEmpty()) dateCreated = Timestamp.getUnixTimestampHex()
  }

  @SerialName("guid")
  var gUID = ""

  @SerialName("cdate")
  var dateCreated = ""
  var payload = ""
  var payloadType = ""
}
