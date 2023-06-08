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
  override var uID: Long,
  @SerialName("t") var title: String,
) : IEntry {
  override fun initialize() {
    if (uID == -1L) uID = uniChatroomIndexManager!!.getUID()
    if (chatroomGUID.isEmpty()) chatroomGUID = Uuid.randomUUID().toString()
    if (dateCreated.isEmpty()) dateCreated = Timestamp.getUnixTimestampHex()
    if (rank == 0) {
      rank = 1
      rankDescription = "Starter"
    }
    if (rolesRequiredRead.isEmpty()) {
      rolesRequiredRead.add("Member")
    }
    if (rolesRequiredWrite.isEmpty()) {
      rolesRequiredWrite.add("Member")
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

  /**
   * List of JSON encoded roles available in this chatroom (+ its subchatrooms if available)
   */
  var roles: ArrayList<String> = arrayListOf()

  @SerialName("writeRoles")
  var rolesRequiredWrite: ArrayList<String> = arrayListOf()

  @SerialName("readRoles")
  var rolesRequiredRead: ArrayList<String> = arrayListOf()
}
