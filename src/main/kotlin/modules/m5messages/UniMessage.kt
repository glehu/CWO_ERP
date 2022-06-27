package modules.m5messages

import com.benasher44.uuid.Uuid
import interfaces.IEntry
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import modules.mx.logic.Timestamp
import modules.mx.uniMessagesIndexManager

@InternalAPI
@ExperimentalSerializationApi
@kotlinx.serialization.Serializable
data class UniMessage(
  override var uID: Int = -1,
  @SerialName("cid")
  var uniChatroomUID: Int,
  @SerialName("src")
  val from: String,
  @SerialName("msg")
  var message: String,
) : IEntry {
  @SerialName("ts")
  var timestamp: String = ""

  var gUID: String = ""

  init {
    if (timestamp.isEmpty()) timestamp = Timestamp.now()
    if (gUID.isEmpty()) gUID = Uuid.randomUUID().toString()
  }

  override fun initialize() {
    if (uID == -1) uID = uniMessagesIndexManager!!.getUID()
  }
}
