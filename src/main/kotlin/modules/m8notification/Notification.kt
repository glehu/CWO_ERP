package modules.m8notification

import com.benasher44.uuid.Uuid
import interfaces.IEntry
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import modules.mx.logic.Timestamp
import modules.mx.notificationIndexManager

@InternalAPI
@ExperimentalSerializationApi
@kotlinx.serialization.Serializable
data class Notification(
  override var uID: Long = -1L, @SerialName("recipient") var recipientUsername: String
) : IEntry {
  @SerialName("t")
  var title: String = ""

  @SerialName("desc")
  var description: String = ""

  @SerialName("author")
  var authorUsername: String = ""

  var hasClickAction: Boolean = false
  var clickAction: String = ""
  var clickActionReferenceGUID: String = ""

  var keywords: String = ""
  var type: String = ""
  var guid: String = ""

  @SerialName("cdate")
  var dateCreated: String = ""

  var finished: Boolean = false
  var finishedDate: String = ""

  var hasAttachment: Boolean = false
  var snippetGUID: String = ""

  var hasBackgroundImage: Boolean = false
  var backgroundSnippetGUID: String = ""

  init {
    if (dateCreated.isEmpty()) dateCreated = Timestamp.now()
    if (guid.isEmpty()) guid = Uuid.randomUUID().toString()
    if (type.isEmpty()) type = "info"
  }

  override fun initialize() {
    if (uID == -1L) uID = notificationIndexManager!!.getUID()
  }
}
