package modules.m7knowledge

import com.benasher44.uuid.Uuid
import interfaces.IEntry
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import modules.mx.knowledgeIndexManager
import modules.mx.logic.Timestamp

@InternalAPI
@ExperimentalSerializationApi
@kotlinx.serialization.Serializable
data class Knowledge(
  override var uID: Int = -1
) : IEntry {
  @SerialName("t")
  var title: String = ""

  @SerialName("desc")
  var description: String = ""
  var keywords: String = ""
  var gUID: String = ""
  var isPrivate: Boolean = true
  var mainChatroomGUID: String = ""
  var categories: ArrayList<String> = arrayListOf()
  var members: ArrayList<String> = arrayListOf()
  var banlist: ArrayList<String> = arrayListOf()
  var rolesToJoin: ArrayList<String> = arrayListOf()
  var rules: String = ""

  @SerialName("cdate")
  var dateCreated: String = ""

  @SerialName("reacts")
  var reactions: ArrayList<String> = arrayListOf()
  var subChatrooms: ArrayList<String> = arrayListOf()

  init {
    if (dateCreated.isEmpty()) dateCreated = Timestamp.getUnixTimestampHex()
    if (gUID.isEmpty()) gUID = Uuid.randomUUID().toString()
  }

  override fun initialize() {
    if (uID == -1) uID = knowledgeIndexManager!!.getUID()
  }
}