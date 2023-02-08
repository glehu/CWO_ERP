package modules.mTemplate

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
data class TemplateEntry(
  override var uID: Long = -1,
) : IEntry {
  @SerialName("ts")
  var timestamp: String = ""

  var gUID: String = ""

  init {
    if (timestamp.isEmpty()) timestamp = Timestamp.now()
    if (gUID.isEmpty()) gUID = Uuid.randomUUID().toString()
  }

  override fun initialize() {
    if (uID == -1L) uID = uniMessagesIndexManager!!.getUID()
  }
}
