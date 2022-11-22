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
  @SerialName("t") var title: String,
) : IEntry {
  override fun initialize() {
    if (uID == -1) uID = uniChatroomIndexManager!!.getUID()
    if (chatroomGUID.isEmpty()) chatroomGUID = Uuid.randomUUID().toString()
    if (dateCreated.isEmpty()) dateCreated = Timestamp.getUnixTimestampHex()
    if (rank == 0) {
      rank = 1
      rankDescription = "Starter"
    }
    // Always keep this up to date
    dateChangedUnix = Timestamp.getUnixTimestamp()
  }

  @SerialName("guid")
  var chatroomGUID: String = ""

  @SerialName("cdate")
  var dateCreated: String = ""

  @SerialName("ts")
  var dateChangedUnix: Long = -1L

  @SerialName("s")
  var status: Int = 1
  var imgGUID: String = ""
  var members: ArrayList<String> = arrayListOf()
  var banlist: ArrayList<String> = arrayListOf()
  var subChatrooms: ArrayList<String> = arrayListOf()
  var parentGUID: String = ""

  /** Determines the type of this chatroom.
   *
   * Possible values (currently):
   * - text
   * - screenshare
   * - webcam
   */
  var type: String = "text"
  var rank: Int = 0
  var rankDescription: String = ""
  var directMessageUsername: String = ""
}
