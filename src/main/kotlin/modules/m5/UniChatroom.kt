package modules.m5

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
data class UniChatroom(
  override var uID: Int,
  @SerialName("t")
  var title: String,
) : IEntry {
  override fun initialize() {
    if (uID == -1) uID = uniChatroomIndexManager!!.getUID()
    if (chatroomGUID == "") chatroomGUID = Uuid.randomUUID().toString()
    if (dateCreated == "") dateCreated = Timestamp.getUnixTimestampHex()
    // Always keep this up to date
    dateChangedUnix = Timestamp.getUnixTimestamp()
  }

  @SerialName("guid")
  var chatroomGUID = ""
  @SerialName("cdate")
  var dateCreated = ""
  @SerialName("ts")
  var dateChangedUnix = -1L
  @SerialName("s")
  var status = 1
  var members: ArrayList<String> = arrayListOf()
  var banlist: ArrayList<String> = arrayListOf()
  var messages: ArrayList<String> = arrayListOf()
  var subChatrooms: ArrayList<String> = arrayListOf()
}
