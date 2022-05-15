package modules.m5messages

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
  val uniChatroomUID: Int,
  @SerialName("src")
  val from: String,
  @SerialName("msg")
  val message: String,
) : IEntry {
  @SerialName("ts")
  var timestamp: String = ""

  init {
    if (timestamp.isEmpty()) timestamp = Timestamp.now()
  }

  override fun initialize() {
    if (uID == -1) uID = uniMessagesIndexManager!!.getUID()
  }
}
