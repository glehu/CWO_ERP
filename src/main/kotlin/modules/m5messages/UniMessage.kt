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
  override var uID: Long = -1L,
  @SerialName("cid") var uniChatroomUID: Long,
  @SerialName("src") val from: String,
  @SerialName("msg") var message: String,
) : IEntry {
  @SerialName("ts")
  var timestamp: String = ""

  var guid: String = ""

  var isEncrypted = false

  @SerialName("reacts")
  var reactions: ArrayList<String> = arrayListOf()

  init {
    if (timestamp.isEmpty()) timestamp = Timestamp.now()
    if (guid.isEmpty()) guid = Uuid.randomUUID().toString()
  }

  override fun initialize() {
    if (uID == -1L) uID = uniMessagesIndexManager!!.getUID()
  }
}
