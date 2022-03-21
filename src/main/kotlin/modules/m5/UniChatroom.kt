package modules.m5

import com.benasher44.uuid.Uuid
import interfaces.IEntry
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.uniChatroomIndexManager

@InternalAPI
@ExperimentalSerializationApi
@kotlinx.serialization.Serializable
data class UniChatroom(
  override var uID: Int,
  var chatroomGUID: String,
  var title: String,
) : IEntry {
  override fun initialize() {
    if (uID == -1) uID = uniChatroomIndexManager!!.getUID()
    if (chatroomGUID == "") chatroomGUID = Uuid.fromString(title).toString()
  }

  var dateCreated = ""
  var status = 1
  var members: MutableMap<String, String> = mutableMapOf()
  var messages: MutableMap<String, String> = mutableMapOf()
}
