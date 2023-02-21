package modules.m9process

import com.benasher44.uuid.Uuid
import interfaces.IEntry
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import modules.mx.uniMessagesIndexManager

@InternalAPI
@ExperimentalSerializationApi
@kotlinx.serialization.Serializable
data class ProcessEvent(
  override var uID: Long = -1,
) : IEntry {
  @SerialName("ts")
  var guid: String = ""

  // Purpose

  /**
   * This event's mode specified by "ProcessController.mode"
   */
  var mode: String = ""

  var actionType: String = ""

  var actionTarget: ArrayList<String> = arrayListOf()

  var actionTargetType: String = ""

  var value: String = ""

  // Description

  var title: String = ""

  var description: String = ""

  var keywords: String = ""

  var triggerWords: String = ""

  var knowledgeGUID: String = ""

  var wisdomGUID: String = ""

  var wisdomReferences: ArrayList<String> = arrayListOf()

  /**
   * If not -1, defines the start of an elaboration path, containing a sub-path being grouped by this entry.
   */
  var elaborationPathStartUID: Long = -1

  // #### In/Out

  val incomingUID: ArrayList<Long> = arrayListOf()

  var outgoingUID: ArrayList<Long> = arrayListOf()

  // #### Timing

  var durationMin: Double = 0.0

  var durationMinUnit: String = "ms"

  var durationMax: Double = 0.0

  var durationMaxUnit: String = "ms"

  var amount: Int = 1

  // #### Likelihood

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
    if (guid.isEmpty()) guid = Uuid.randomUUID().toString()
  }

  override fun initialize() {
    if (uID == -1L) uID = uniMessagesIndexManager!!.getUID()
  }
}
