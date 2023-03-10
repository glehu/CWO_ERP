package modules.m9process.logic

import api.logic.core.ServerController
import api.misc.json.ProcessEntryConfig
import api.misc.json.ProcessEventsPayload
import api.misc.json.ProcessInteractionPayload
import api.misc.json.ProcessPathFullSegmentPayload
import api.misc.json.ProcessPathPayload
import api.misc.json.ProcessPathSegmentPayload
import api.misc.json.QueryResult
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
import modules.m2.Contact
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
    authorFilter: String,
    queryFilter: String
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
    var regexPattern: Regex? = null
    var doQuery = false
    if (queryFilter.isNotEmpty()) {
      regexPattern = buildQueryRegexPattern(queryFilter)
      doQuery = true
    }
    // Retrieve processes
    val processes: ArrayList<ProcessEvent> = arrayListOf()
    val results: ArrayList<QueryResult> = arrayListOf()
    var queryResult: QueryResult
    getEntriesFromIndexSearch("^$modeFilter\\|${knowledgeRef!!.uID}$", 2, true) {
      it as ProcessEvent
      if (authorFilter.isEmpty() || it.authorUsername.contains("^$authorFilter$".toRegex())) {
        it.dateCreated = Timestamp.getUTCTimestampFromHex(it.dateCreated)
        if (doQuery) {
          queryResult = applyQueryRegexPattern(
                  regexPattern!!, it, it.title, it.keywords, it.description, it.authorUsername)

          if (queryResult.rating > 0) results.add(queryResult)
        } else {
          processes.add(it)
        }
      }
    }
    if (doQuery) {
      // Put results back into the response payload array
      val sortedResults = sortQueryResults(results)
      for (sortedEntry in sortedResults) {
        processes.add(sortedEntry.entry as ProcessEvent)
      }
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
    val segment = ProcessPathFullSegmentPayload(processEvent)
    // Related entries?
    val wisdomController = WisdomController()
    var wisdom: Wisdom
    if (processEvent.wisdomUID != -1L) {
      try {
        wisdom = wisdomController.get(processEvent.wisdomUID) as Wisdom
        segment.wisdom = wisdom
      } catch (_: Exception) {
      }
    }
    if (processEvent.taskWisdomUID != -1L) {
      try {
        wisdom = wisdomController.get(processEvent.taskWisdomUID) as Wisdom
        segment.task = wisdom
      } catch (_: Exception) {
      }
    }
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
      var altSegment: ProcessPathSegmentPayload
      for (i in 1 until processEvent.outgoingUID.size) {
        try {
          alternateEvent = get(processEvent.outgoingUID[i]) as ProcessEvent
          altSegment = ProcessPathSegmentPayload(alternateEvent)
          // Related entries?
          if (alternateEvent.wisdomUID != -1L) {
            try {
              wisdom = wisdomController.get(alternateEvent.wisdomUID) as Wisdom
              altSegment.wisdom = wisdom
            } catch (_: Exception) {
            }
          }
          if (alternateEvent.taskWisdomUID != -1L) {
            try {
              wisdom = wisdomController.get(alternateEvent.taskWisdomUID) as Wisdom
              altSegment.task = wisdom
            } catch (_: Exception) {
            }
          }
          // Add
          segment.alternatives.add(altSegment)
        } catch (_: Exception) {
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
    if (config.action.trim().contains("^generate_documentation$".toRegex())) {
      val success = httpGenerateMarkdownDocumentation(appCall, processEvent!!)
      if (!success) {
        appCall.respond(HttpStatusCode.InternalServerError)
        return
      }
    } else if (config.action.trim().contains("^create_tasks$".toRegex())) {
      val success = httpCreateTasks(appCall, processEvent!!)
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
        sbBody.append("Author: ${segment.event.authorUsername}  $nl")
        sbBody.append("Date: " + Timestamp.getUTCTimestampFromHex(segment.event.dateCreated) + nnl)
      } else {
        // Skip empty values that also do not contain alternatives
        if (segment.event.title.isEmpty() && segment.event.description.isEmpty() && segment.alternatives.isEmpty()) {
          continue
        }
      }
      // Append to contents and body...
      if (index >= 1) sbBody.append("---$nnl") // Separator if there was a previous entry
      if (segment.event.title.isNotEmpty()) {
        sbContents.append("${index + 1}. " + segment.event.title + nl)
        sbBody.append("## ${index + 1}. " + segment.event.title + nnl)
      } else {
        sbContents.append("${index + 1}. (No Title)$nl")
        sbBody.append("## ${index + 1}. (No Title)$nnl")
      }
      if (segment.event.description.isNotEmpty()) {
        sbBody.append(segment.event.description + nnl)
      } else {
        sbBody.append("(No Description)$nnl")
      }
      // Print alternatives, too
      if (segment.alternatives.isNotEmpty()) {
        for ((indexAlt, altEvent) in segment.alternatives.withIndex()) {
          // Skip empty entries
          if (altEvent.event.title.isEmpty() && altEvent.event.description.isEmpty()) continue
          // Append to contents and body...
          if (altEvent.event.title.isNotEmpty()) {
            sbContents.append("* ${index + 1}.${indexAlt + 1}. " + altEvent.event.title + nl)
            sbBody.append("### ${index + 1}.${indexAlt + 1}. " + altEvent.event.title + nnl)
          } else {
            sbContents.append("* ${index + 1}.${indexAlt + 1}. (No Title)$nl")
            sbBody.append("### ${index + 1}.${indexAlt + 1}. (No Title)$nnl")
          }
          if (altEvent.event.description.isNotEmpty()) {
            sbBody.append(altEvent.event.description + nnl)
          } else {
            sbBody.append("(No Description)$nnl")
          }
        }
      }
    }
    sbContents.append("$nl---$nnl")
    appCall.respond(sbTitle.toString() + sbContents.toString() + sbBody.toString())
    return true
  }

  private suspend fun httpCreateTasks(
    appCall: ApplicationCall,
    processEvent: ProcessEvent
  ): Boolean {
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
    val knowledgeController = KnowledgeController()
    val knowledgeRef: Knowledge
    try {
      knowledgeRef = knowledgeController.load(processEvent.knowledgeUID) as Knowledge
    } catch (e: Exception) {
      appCall.respond(HttpStatusCode.BadRequest)
      return true
    }
    if (!knowledgeController.httpCanAccessKnowledge(appCall, knowledgeRef)) {
      return true
    }
    // Create tasks (and a box) using the full path of this processEvent
    val path = getEventsOfProcess(processEvent)
    val wisdomController = WisdomController()
    var box: Wisdom? = null
    var task: Wisdom?
    var boxUID = -1L
    var taskUID: Long
    for ((index, segment) in path.path.withIndex()) {
      if (index == 0) {
        // Check if the box exists
        if (segment.event.taskWisdomUID == -1L) {
          // Box does not exist -> Create it
          box = Wisdom(-1)
          box.type = "box"
          box.knowledgeUID = knowledgeRef.uID
          box.authorUsername = user!!.username
          if (segment.event.title.isNotEmpty()) {
            box.title = segment.event.title
          } else {
            box.title = "(No Title)"
          }
          if (segment.event.description.isNotEmpty()) {
            box.description = segment.event.description
          } else {
            box.description = "(No Description)"
          }
          box.keywords = segment.event.keywords
          box.isTask = true
          box.taskType = "box"
          boxUID = wisdomController.saveEntry(box)
          box.uID = boxUID
          // Set boxUID and save process event
          // We retrieve the latest entry to not override any changes made during this operation
          segment.event = get(segment.event.uID) as ProcessEvent
          segment.event.taskWisdomUID = boxUID
          saveEntry(segment.event)
        } else {
          // Box exists -> Retrieve and update it
          try {
            box = wisdomController.get(segment.event.taskWisdomUID) as Wisdom
            if (segment.event.title.isNotEmpty()) {
              box.title = segment.event.title
            } else {
              box.title = "(No Title)"
            }
            if (segment.event.description.isNotEmpty()) {
              box.description = segment.event.description
            } else {
              box.description = "(No Description)"
            }
            boxUID = wisdomController.saveEntry(box)
          } catch (e: Exception) {
            appCall.respond(HttpStatusCode.BadRequest)
            return true
          }
        }
        if (segment.alternatives.isNotEmpty()) {
          for (altEvent in segment.alternatives) {
            checkEventTask(altEvent.event, knowledgeRef, box, user!!, wisdomController)
          }
        }
        continue
      }
      // Continue with tasks
      // Check box and boxUID first since we need those for referencing
      if (box == null || boxUID == -1L) {
        appCall.respond(HttpStatusCode.ExpectationFailed)
        return true
      }
      checkEventTask(segment.event, knowledgeRef, box, user!!, wisdomController)
      if (segment.alternatives.isNotEmpty()) {
        for (altEvent in segment.alternatives) {
          checkEventTask(altEvent.event, knowledgeRef, box, user, wisdomController)
        }
      }
    }
    appCall.respond(box!!.guid)
    return true
  }

  private suspend fun checkEventTask(
    event: ProcessEvent,
    knowledgeRef: Knowledge,
    box: Wisdom,
    user: Contact,
    wisdomController: WisdomController
  ) {
    if (event.title.isEmpty() && event.description.isEmpty()) return
    val task: Wisdom
    if (event.taskWisdomUID == -1L) {
      // Task does not exist yet -> Create it
      task = Wisdom(-1)
      task.type = "task"
      task.knowledgeUID = knowledgeRef.uID
      task.authorUsername = user.username
      if (event.title.isNotEmpty()) {
        task.title = event.title
      } else {
        task.title = "(No Title)"
      }
      if (event.description.isNotEmpty()) {
        task.description = event.description
      } else {
        task.description = "(No Description)"
      }
      task.keywords = event.keywords
      task.isTask = true
      task.taskType = "task"
      task.srcWisdomUID = box.uID
      task.keywords += "," + box.title
      val taskUID = wisdomController.saveEntry(task)
      // Set taskUID and save process event
      // We retrieve the latest entry to not override any changes made during this operation
      val eventTmp = get(event.uID) as ProcessEvent
      eventTmp.taskWisdomUID = taskUID
      saveEntry(eventTmp)
    } else {
      // It exists -> Retrieve and update it
      task = wisdomController.get(event.taskWisdomUID) as Wisdom
      if (event.title.isNotEmpty()) {
        task.title = event.title
      } else {
        task.title = "(No Title)"
      }
      if (event.description.isNotEmpty()) {
        task.description = event.description
      } else {
        task.description = "(No Description)"
      }
      task.keywords = event.keywords
      wisdomController.saveEntry(task)
    }
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
