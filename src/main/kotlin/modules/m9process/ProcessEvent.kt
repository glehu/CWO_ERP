package modules.m9process

import com.benasher44.uuid.Uuid
import interfaces.IEntry
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import modules.mx.logic.Timestamp
import modules.mx.processIndexManager

@InternalAPI
@ExperimentalSerializationApi
@kotlinx.serialization.Serializable
data class ProcessEvent(
  override var uID: Long = -1L,
) : IEntry {
  var guid: String = ""

  // Purpose

  /**
   * This event's mode specified by "ProcessController.Mode"
   */
  var mode: String = ""

  /**
   * This event's mode specified by "ProcessController.ActionType"
   */
  var actionType: String = "incoming"

  var actionTarget: ArrayList<String> = arrayListOf()

  var actionTargetType: String = ""

  var value: String = ""

  // Description

  @SerialName("t")
  var title: String = ""

  @SerialName("author")
  var authorUsername: String = ""

  var collaborators: ArrayList<String> = arrayListOf()

  @SerialName("desc")
  var description: String = ""

  var keywords: String = ""

  @SerialName("cdate")
  var dateCreated: String = ""

  var knowledgeUID: Long = -1L

  var wisdomUID: Long = -1L

  var taskWisdomUID: Long = -1L

  var wisdomReferences: ArrayList<String> = arrayListOf()

  /**
   * If not -1, defines the start of an elaboration path, containing a sub-path being grouped by this entry.
   */
  var elaborationPathStartUID: Long = -1L

  // #### In/Out

  val incomingUID: ArrayList<Long> = arrayListOf()

  var outgoingUID: ArrayList<Long> = arrayListOf()

  var rowIndex: Int = 0

  // #### Timing

  var graceTime: Double = 0.0

  var graceTimeUnit: String = "ms"

  var durationMin: Double = 0.0

  var durationMinUnit: String = "ms"

  var durationMax: Double = 0.0

  var durationMaxUnit: String = "ms"

  var amount: Int = 1

  // #### Likelihood

  var auto: Boolean = false

  var triggerWords: String = ""

  /**
   * Defines the weighting of this entry.
   */
  var weight: Double = 1.0

  var optional: Boolean = false

  /**
   * Defines the chance of this event triggering from 1 being the highest and 0 the lowest.
   */
  var probability: Int = 1

  init {
    if (dateCreated.isEmpty()) dateCreated = Timestamp.getUnixTimestampHex()
    if (guid.isEmpty()) guid = Uuid.randomUUID().toString()
  }

  override fun initialize() {
    if (uID == -1L) uID = processIndexManager!!.getUID()
  }
}
