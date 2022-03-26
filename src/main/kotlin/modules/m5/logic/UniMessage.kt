package modules.m5.logic

import com.benasher44.uuid.Uuid
import kotlinx.serialization.SerialName
import modules.mx.logic.Timestamp

@kotlinx.serialization.Serializable
data class UniMessage(
  @SerialName("from")
  val from: String,
  @SerialName("message")
  val message: String
) {
  @SerialName("uniChatroomGUID")
  val messageGUID: String = Uuid.randomUUID().toString()

  @SerialName("timestamp")
  val timestamp: String = Timestamp.getUnixTimestampHex()

}
