package modules.m5.logic

import com.benasher44.uuid.Uuid

@kotlinx.serialization.Serializable
data class UniMessage(
  var from: String,
  var timestamp: String,
  var message: String
) {
  val messageGUID: String = Uuid.randomUUID().toString()
}
