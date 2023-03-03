package modules.m9process.logic

import api.logic.core.ServerController
import api.misc.json.ProcessEntryConfig
import api.misc.json.ProcessEventsPayload
import api.misc.json.ProcessInteractionPayload
import api.misc.json.ProcessPathPayload
import api.misc.json.ProcessPathSegmentPayload
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.m7knowledge.Knowledge
import modules.m7knowledge.logic.KnowledgeController
import modules.m7wisdom.Wisdom
import modules.m7wisdom.WisdomCollaborator
import modules.m7wisdom.logic.WisdomController
import modules.m9process.ProcessEvent
import modules.mx.logic.Timestamp
import modules.mx.logic.UserCLIManager
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
    config: ProcessEntryConfig,
    processGUID: String,
    mode: String
  ) {
    // Are we editing?
    if (mode.contains("^edit$".toRegex())) {
      httpEditProcessEvent(appCall, config, processGUID, mode)
      return
    }
    // Check config
    val modeList = getModeList()
    if (config.mode.isEmpty()) {
      if (config.previousEventGUID.isEmpty()) {
        config.mode = Mode.START.toString
      } else {
        config.mode = Mode.EVENT.toString
      }
    }
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
    var nextEvent: ProcessEvent? = null
    if (config.nextEventGUID.isNotEmpty()) {
      getEntriesFromIndexSearch("^${config.nextEventGUID}$", 1, true) {
        it as ProcessEvent
        nextEvent = it
      }
      if (nextEvent == null) {
        appCall.respond(HttpStatusCode.NotFound)
        return
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
    event.authorUsername = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))!!.username
    // --- References
    event.knowledgeUID = knowledgeRef!!.uID
    if (wisdom != null && wisdom!!.uID != -1L) event.wisdomUID = wisdom!!.uID
    if (task != null && task!!.uID != -1L) event.taskWisdomUID = task!!.uID
    if (previousEvent != null) {
      // Check if event contains previous event uID already; add if not present
      if (!event.incomingUID.contains(previousEvent!!.uID)) {
        event.incomingUID.add(previousEvent!!.uID)
      }
    }
    if (nextEvent != null) {
      // Check if event contains next event uID already; add if not present
      if (!event.outgoingUID.contains(nextEvent!!.uID)) {
        event.outgoingUID.add(nextEvent!!.uID)
      }
    }
    // --- More...
    event.title = config.title
    event.description = config.description
    event.keywords = config.keywords
    event.value = config.value
    event.actionType = config.actionType
    event.mode = config.mode
    // Save event
    val uID = saveEntry(event)
    // Update the previous event if it exists
    if (previousEvent != null) {
      if (!previousEvent!!.outgoingUID.contains(uID)) {
        previousEvent!!.outgoingUID.add(uID)
        saveEntry(previousEvent!!)
      }
    }
    // Update the next event if it exists
    if (nextEvent != null) {
      if (!nextEvent!!.incomingUID.contains(uID)) {
        nextEvent!!.incomingUID.add(uID)
        saveEntry(nextEvent!!)
      }
    }
    appCall.respond(event.guid)
  }

  private suspend fun httpEditProcessEvent(
    appCall: ApplicationCall,
    config: ProcessEntryConfig,
    processGUID: String,
    mode: String
  ) {
    var processEvent: ProcessEvent? = null
    if (processGUID.isEmpty()) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    // Get existing entry
    getEntriesFromIndexSearch(
            searchText = "^$processGUID$", ixNr = 1, showAll = true) {
      it as ProcessEvent
      processEvent = it
    }
    if (processEvent === null) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    // Edit
    processEvent!!.title = config.title
    processEvent!!.description = config.description
    if (config.wisdomGUID.isNotEmpty()) {
      if (config.wisdomGUID == "-1") {
        processEvent!!.wisdomUID = -1L
      } else {
        var wisdom: Wisdom? = null
        WisdomController().getEntriesFromIndexSearch("^${config.wisdomGUID}$", 1, true) {
          it as Wisdom
          wisdom = it
        }
        if (wisdom == null) {
          appCall.respond(HttpStatusCode.NotFound)
          return
        }
        // If wisdom was already referenced, check if it changed
        if (processEvent!!.wisdomUID != -1L) {
          if (processEvent!!.wisdomUID != wisdom!!.uID) {
            // New wisdom needs to replace old one -> we delete the old one
            val wisdomOld: Wisdom
            try {
              wisdomOld = WisdomController().get(processEvent!!.wisdomUID) as Wisdom
              wisdomOld.guid = ""
              wisdomOld.knowledgeUID = -1
              wisdomOld.title = ""
              wisdomOld.description = ""
              wisdomOld.srcWisdomUID = -1L
              wisdomOld.refWisdomUID = -1L
              wisdomOld.isTask = false
              WisdomController().saveEntry(wisdomOld)
            } catch (_: Exception) {
              // Entry could not be loaded
            }
          }
        }
        processEvent!!.wisdomUID = wisdom!!.uID
      }
    }
    // Save and respond
    saveEntry(processEvent!!)
    appCall.respond(HttpStatusCode.OK)
  }

  suspend fun httpGetProcesses(
    appCall: ApplicationCall,
    knowledgeGUID: String,
    modeFilter: String,
    authorFilter: String
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
      if (authorFilter.isEmpty() || it.authorUsername.contains("^$authorFilter$".toRegex())) processes.add(it)
    }
    if (processes.isEmpty()) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    appCall.respond(processes)
  }

  suspend fun httpGetEventsOfProcess(
    appCall: ApplicationCall,
    knowledgeGUID: String,
    entryPointGUID: String
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
    // Retrieve entry point event
    var entryPoint: ProcessEvent? = null
    getEntriesFromIndexSearch("^$entryPointGUID$", 1, true) {
      it as ProcessEvent
      entryPoint = it
    }
    if (entryPoint == null) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    // Retrieve processes linking to the provided entry point event
    val processes = ProcessEventsPayload(entryPoint!!)
    var entry: ProcessEvent
    if (entryPoint!!.incomingUID.isNotEmpty()) {
      for (uid in entryPoint!!.incomingUID) {
        try {
          entry = load(uid) as ProcessEvent
          processes.incoming.add((entry))
        } catch (_: Exception) {
          // Entry could not get loaded!
        }
      }
    }
    if (entryPoint!!.outgoingUID.isNotEmpty()) {
      for (uid in entryPoint!!.outgoingUID) {
        try {
          entry = load(uid) as ProcessEvent
          processes.outgoing.add((entry))
        } catch (_: Exception) {
          // Entry could not get loaded!
        }
      }
    }
    // Return the results
    appCall.respond(processes)
  }

  suspend fun httpDeleteProcessEvent(
    appCall: ApplicationCall,
    processEventGUID: String?
  ) {
    var processEvent: ProcessEvent? = null
    getEntriesFromIndexSearch("^$processEventGUID$", 1, true) {
      it as ProcessEvent
      processEvent = it
    }
    if (processEvent == null) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    // Check user
    val username = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))!!.username
    if (processEvent!!.authorUsername != username && !processEvent!!.isCollaborator(username)) {
      appCall.respond(HttpStatusCode.Forbidden)
      return
    }
    // Retrieve and check knowledge
    if (processEvent!!.knowledgeUID != -1L) {
      val knowledgeController = KnowledgeController()
      val knowledgeRef: Knowledge
      try {
        knowledgeRef = knowledgeController.load(processEvent!!.knowledgeUID) as Knowledge
      } catch (e: Exception) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      if (!knowledgeController.httpCanAccessKnowledge(appCall, knowledgeRef)) {
        return
      }
    }
    // Check if this process has in-/outgoing events
    var tmpEntry: ProcessEvent?
    if (processEvent!!.incomingUID.isNotEmpty()) {
      for (uid in processEvent!!.incomingUID) {
        try {
          tmpEntry = get(uid) as ProcessEvent
          tmpEntry.outgoingUID.remove(processEvent!!.uID)
          saveEntry(tmpEntry)
        } catch (_: Exception) {
          // Entry could not get loaded!
        }
      }
    }
    if (processEvent!!.outgoingUID.isNotEmpty()) {
      for (uid in processEvent!!.outgoingUID) {
        try {
          tmpEntry = get(uid) as ProcessEvent
          tmpEntry.incomingUID.remove(processEvent!!.uID)
          if (tmpEntry.incomingUID.isEmpty()) {
            // Can we close this gap?
            /* Since we removed all incoming connections of this remote entry,
            we are risking of having floating events from this point onwards
            Example:
                1 - 2 - 3 - 4
              We deleted entry #3 while currently looking at outgoing entry #4, thus creating this gap:
                1 - 2 -   - 4
              Since this is a linear chain, we can simply reorganize
              the in-/outgoing connections of both edge entries:
                1 - 2 - 4
             */
            if (processEvent!!.incomingUID.isNotEmpty() && processEvent!!.incomingUID.size == 1) {
              tmpEntry.incomingUID.add(processEvent!!.incomingUID[0])
            }
          }
          // Save
          saveEntry(tmpEntry)
        } catch (_: Exception) {
          // Entry could not get loaded!
        }
      }
    }
    // Delete process event by removing all indexed fields' values
    processEvent!!.knowledgeUID = -1L
    processEvent!!.wisdomUID = -1L
    processEvent!!.taskWisdomUID = -1L
    processEvent!!.guid = ""
    // Save and respond
    saveEntry(processEvent!!)
    appCall.respond(HttpStatusCode.OK)
  }

  suspend fun httpGetFullProcessPath(
    appCall: ApplicationCall,
    knowledgeGUID: String,
    entryPointGUID: String
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
    // Retrieve entry point event
    var entryPoint: ProcessEvent? = null
    getEntriesFromIndexSearch("^$entryPointGUID$", 1, true) {
      it as ProcessEvent
      entryPoint = it
    }
    if (entryPoint == null) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    // We now need to retrieve all outgoing connections thus constructing the full path
    val processes = getEventsOfProcess(entryPoint!!)
    appCall.respond(processes)
  }

  /**
   * Recursively retrieves all events following the specified [ProcessEvent].
   * @return [ProcessPathPayload]
   */
  private tailrec fun getEventsOfProcess(
    processEvent: ProcessEvent,
    processes: ProcessPathPayload = ProcessPathPayload()
  ): ProcessPathPayload {
    val segment = ProcessPathSegmentPayload(processEvent)
    // Investigate the provided event...
    if (processEvent.outgoingUID.isEmpty()) {
      // There are no next events!
      // Return what we have retrieved so far
      processes.path.add(segment)
      return processes
    }
    val nextEvent: ProcessEvent
    try {
      // Retrieve next event and update payload
      nextEvent = get(processEvent.outgoingUID[0]) as ProcessEvent
    } catch (_: Exception) {
      // Entry could not be retrieved -> Exit
      processes.path.add(segment)
      return processes
    }
    // Add alternatives if they exist
    if (processEvent.outgoingUID.size > 1) {
      // We skip one since we're only looking for alternatives to the first (index 0)
      var alternateEvent: ProcessEvent
      for (i in 1 until processEvent.outgoingUID.size) {
        try {
          alternateEvent = get(processEvent.outgoingUID[i]) as ProcessEvent
          segment.alternatives.add(alternateEvent)
        } catch (_: Exception) {
          // Entry could not be retrieved
        }
      }
    }
    processes.path.add(segment)
    return getEventsOfProcess(nextEvent, processes)
  }

  suspend fun httpInteractProcessEvent(
    appCall: ApplicationCall,
    processEventGUID: String?,
    config: ProcessInteractionPayload
  ) {
    if (config.action.isEmpty()) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    // Retrieve event
    var processEvent: ProcessEvent? = null
    getEntriesFromIndexSearch("^$processEventGUID$", 1, true) {
      it as ProcessEvent
      processEvent = it
    }
    if (processEvent == null) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    // Retrieve and check knowledge
    if (processEvent!!.knowledgeUID != -1L) {
      val knowledgeController = KnowledgeController()
      val knowledgeRef: Knowledge
      try {
        knowledgeRef = knowledgeController.load(processEvent!!.knowledgeUID) as Knowledge
      } catch (e: Exception) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      if (!knowledgeController.httpCanAccessKnowledge(appCall, knowledgeRef)) {
        return
      }
    }
    // Analyze action
    if (config.action.trim().contains("^doc$".toRegex())) {
      val success = httpGenerateMarkdownDocumentation(appCall, processEvent!!)
      if (!success) {
        appCall.respond(HttpStatusCode.InternalServerError)
        return
      }
    }
  }

  private suspend fun httpGenerateMarkdownDocumentation(
    appCall: ApplicationCall,
    processEvent: ProcessEvent
  ): Boolean {
    val nnl = "\n\n" // Two linebreaks because of Markdown paragraphs
    val nl = "\n" // Single linebreak for lists
    val sbTitle = StringBuilder()
    val sbContents = StringBuilder()
    val sbBody = StringBuilder()
    // Generate a Markdown documentation using the full path of this processEvent
    val path = getEventsOfProcess(processEvent)
    sbContents.append("## Contents$nnl")
    for ((index, segment) in path.path.withIndex()) {
      if (index == 0) {
        // Append to title
        sbTitle.append("# " + segment.event.title + nnl)
        sbBody.append("Date: " + Timestamp.getUTCTimestampFromHex(segment.event.dateCreated) + "  " + nl)
        sbBody.append("Author: ${segment.event.authorUsername}$nnl")
      }
      // Append to contents...
      sbContents.append("${index + 1}. " + segment.event.title + nl)
      // ... and body
      if (index >= 1) sbBody.append("---$nnl") // Separator if there was a previous entry
      sbBody.append("## ${index + 1}. " + segment.event.title + nnl)
      sbBody.append(segment.event.description + nnl)
      if (segment.alternatives.isNotEmpty()) {
        for ((indexAlt, altEvent) in segment.alternatives.withIndex()) {
          // Append to contents...
          sbContents.append("* ${index + 1}.${indexAlt + 1}. " + altEvent.title + nl)
          // ... and body
          sbBody.append("### ${index + 1}.${indexAlt + 1}. " + altEvent.title + nnl)
          sbBody.append(altEvent.description + nnl)
        }
      }
    }
    sbContents.append("$nl---$nnl")
    appCall.respond(sbTitle.toString() + sbContents.toString() + sbBody.toString())
    return true
  }

  private fun ProcessEvent.isCollaborator(
    username: String,
    removeIfFound: Boolean = false
  ): Boolean {
    if (username.isEmpty()) return false
    if (this.collaborators.isEmpty()) return false
    var collaborator: WisdomCollaborator
    for (i in 0 until this.collaborators.size) {
      collaborator = Json.decodeFromString(this.collaborators[i])
      if (collaborator.username == username) {
        if (removeIfFound) {
          this.collaborators.removeAt(i)
        }
        return true
      }
    }
    return false
  }
}
