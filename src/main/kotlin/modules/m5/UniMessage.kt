package modules.m5

import com.benasher44.uuid.Uuid
import kotlinx.serialization.SerialName

@kotlinx.serialization.Serializable
data class UniMessage(
  @SerialName("from")
  val from: String,
  @SerialName("message")
  val message: String,
  @SerialName("timestamp")
  val timestamp: String
) {
  @SerialName("uniChatroomGUID")
  val messageGUID: String = Uuid.randomUUID().toString()
}
