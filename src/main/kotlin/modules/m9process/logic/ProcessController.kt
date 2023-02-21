package modules.m9process.logic

import api.misc.json.ProcessEntryConfig
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m7knowledge.Knowledge
import modules.m7knowledge.logic.KnowledgeController
import modules.m7wisdom.logic.WisdomController
import modules.m9process.ProcessEvent
import modules.mx.processIndexManager

@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
class ProcessController : IModule {
  override val moduleNameLong = "ProcessController"
  override val module = "M9PROCESS"
  override fun getIndexManager(): IIndexManager {
    return processIndexManager!!
  }

  companion object {
    val mutex = Mutex()
  }

  private suspend fun saveEntry(event: ProcessEvent): Long {
    var uID: Long
    mutex.withLock {
      uID = save(event)
    }
    return uID
  }

  enum class Mode(val toString: String) {
    /**
     * Defines a start of a process.
     */
    START("start"),

    /**
     * Defines an event. Details are to be specified in the entry itself.
     */
    EVENT("event"),

    /**
     * Defines the result of an event. Compared to the END mode, this result continues the path.
     */
    RESULT("result"),

    /**
     * Defines an event that splits the previous process path by creating a new one.
     *
     * This usually means that there are multiple options after reaching a certain point in the process.
     */
    DEVIATION("deviation"),

    /**
     * Defines a risk event that might or might not occur after reaching a certain point in the process.
     */
    RISK("risk"),

    /**
     * Defines a requirement that needs to be fulfilled before continuing the process.
     */
    REQUIREMENT("requirement"),

    /**
     * Defines an end of a process.
     */
    END("end")
  }

  private fun getModeList(): ArrayList<String> {
    val list = arrayListOf<String>()
    enumValues<Mode>().forEach { list.add(it.toString) }
    return list
  }

  enum class ActionType(val toString: String) {
    /**
     * Adds something to a value.
     */
    ADD("add"),

    APPEND("append"),

    /**
     * Removes something from a value.
     */
    SUBTRACT("subtract"),

    REMOVE("remove"),

    /**
     * Modifies some property of a value.
     */
    MODIFY("modify"),

    /**
     * Compares something to a value.
     */
    COMPARE("compare"),

    /**
     * Gathers something.
     */
    GATHER("gather"),

    /**
     * Awaits something.
     */
    AWAIT("await"),

    /**
     * Notifies.
     */
    NOTIFY("notify"),

    /**
     * Runs macro script.
     */
    MACRO("macro")
  }

  private fun getActionTypeList(): ArrayList<String> {
    val list = arrayListOf<String>()
    enumValues<ActionType>().forEach { list.add(it.toString) }
    return list
  }

  enum class ActionTargetType(val toString: String) {
    /**
     * Targets incoming paths.
     */
    INCOMING("incoming"),

    /**
     * Targets a guid, most likely from the CWO_ERP database
     */
    GUID("guid")
  }

  enum class ActionTargetSelectionType(val toString: String) {
    GREEDY("greedy"), SPECIFIC("specific")
  }

  suspend fun httpGetModesList(appCall: ApplicationCall) {
    appCall.respond(getModeList())
  }

  suspend fun httpGetActionTypesList(appCall: ApplicationCall) {
    appCall.respond(getActionTypeList())
  }

  suspend fun httpCreateProcessEvent(
    appCall: ApplicationCall,
    config: ProcessEntryConfig
  ) {
    // Check config
    val modeList = getModeList()
    if (config.mode.isNotEmpty() && !modeList.contains(config.mode)) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    val actionTypeList = getActionTypeList()
    if (config.actionType.isNotEmpty() && !actionTypeList.contains(config.actionType)) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    var previousEvent: ProcessEvent? = null
    mutex.withLock {
      if (config.previousEventGUID.isNotEmpty()) {
        getEntriesFromIndexSearch("^${config.previousEventGUID}$", 1, true) {
          it as ProcessEvent
          previousEvent = it
        }
        if (previousEvent == null) {
          appCall.respond(HttpStatusCode.NotFound)
          return
        }
      }
    }
    // Retrieve and check knowledge
    var knowledgeRef: Knowledge? = null
    KnowledgeController().getEntriesFromIndexSearch(
            searchText = "^${config.knowledgeGUID}$", ixNr = 1, showAll = true) {
      it as Knowledge
      knowledgeRef = it
    }
    if (knowledgeRef == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    if (!KnowledgeController().httpCanAccessKnowledge(appCall, knowledgeRef!!)) return
    // Retrieve and check wisdom if provided
    var wisdom: ProcessEvent? = null
    if (config.wisdomGUID.isNotEmpty()) {
      WisdomController().getEntriesFromIndexSearch("^${config.wisdomGUID}$", 1, true) {
        it as ProcessEvent
        wisdom = it
      }
      if (wisdom == null) {
        appCall.respond(HttpStatusCode.NotFound)
        return
      }
    }
    // Retrieve and check task (wisdom) if provided
    var task: ProcessEvent? = null
    if (config.wisdomGUID.isNotEmpty()) {
      WisdomController().getEntriesFromIndexSearch("^${config.taskGUID}$", 1, true) {
        it as ProcessEvent
        task = it
      }
      if (task == null) {
        appCall.respond(HttpStatusCode.NotFound)
        return
      }
    }
    // Create process event and set it up
    val event = ProcessEvent(-1)
    // References
    event.knowledgeUID = knowledgeRef!!.uID
    if (wisdom!!.uID != -1L) event.wisdomUID = wisdom!!.uID
    if (task!!.uID != -1L) event.taskWisdomUID = task!!.uID
    // Check if event contains previous event uID already; add if not present
    if (previousEvent != null) {
      if (!event.incomingUID.contains(previousEvent!!.uID)) {
        event.incomingUID.add(previousEvent!!.uID)
      }
    }
    // More...
    event.title = config.title
    event.description = config.description
    event.keywords = config.keywords
    event.value = config.value
    event.actionType = config.actionType
    // Save event
    val uID = saveEntry(event)
    // Update the previous event if it exists
    if (previousEvent != null) {
      if (previousEvent!!.outgoingUID.contains(uID)) {
        previousEvent!!.outgoingUID.add(uID)
        saveEntry(previousEvent!!)
      }
    }
    appCall.respond(event.guid)
  }

  suspend fun httpGetProcesses(
    appCall: ApplicationCall,
    knowledgeGUID: String,
    modeFilter: String
  ) {
    // Retrieve and check knowledge
    var knowledgeRef: Knowledge? = null
    KnowledgeController().getEntriesFromIndexSearch(
            searchText = "^$knowledgeGUID$", ixNr = 1, showAll = true) {
      it as Knowledge
      knowledgeRef = it
    }
    if (knowledgeRef == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    if (!KnowledgeController().httpCanAccessKnowledge(appCall, knowledgeRef!!)) return
    // Retrieve processes
    val processes: ArrayList<ProcessEvent> = arrayListOf()
    getEntriesFromIndexSearch("^$modeFilter\\|${knowledgeRef!!.uID}$", 2, true) {
      it as ProcessEvent
      processes.add(it)
    }
    if (processes.isEmpty()) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    appCall.respond(processes)
  }
}
