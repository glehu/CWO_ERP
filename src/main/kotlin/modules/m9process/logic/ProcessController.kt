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

  suspend fun saveEntry(
    event: ProcessEvent,
    indexToDisk: Boolean = true
  ): Long {
    var uID: Long
    mutex.withLock {
      uID = save(event, indexWriteToDisk = indexToDisk)
    }
    return uID
  }

  fun getProcessFromGUID(processGUID: String): ProcessEvent? {
    var processEvent: ProcessEvent? = null
    getEntriesFromIndexSearch(
            searchText = "^$processGUID$", ixNr = 1, showAll = true) {
      it as ProcessEvent
      processEvent = it
    }
    return processEvent
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
    if (mode.contains("^edit$".toRegex()) || mode.contains("^row$".toRegex())) {
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
    // Check rowIndex
    if (event.rowIndex == 0) {
      // Are we moving an alternate event or a main event?
      var entry: ProcessEvent
      var highestRowIndex = 0
      var prevMainEvent: ProcessEvent? = null
      if (event.incomingUID.isNotEmpty()) prevMainEvent = load(event.incomingUID[0]) as ProcessEvent
      if (prevMainEvent != null && prevMainEvent.outgoingUID.isEmpty()) {
        event.rowIndex = prevMainEvent.rowIndex + 20_000
      } else if (prevMainEvent == null || prevMainEvent.outgoingUID[0] == event.uID) {
        // Main event since current event's uID is first outgoing uID
        // If we have a previous event then we need to go back further to find the first event
        if (prevMainEvent != null) {
          // Find first event in this chain
          var incomingUID = -2L
          while (incomingUID != -1L) {
            entry = load(prevMainEvent.incomingUID[0]) as ProcessEvent
            incomingUID = if (entry.incomingUID.isNotEmpty()) {
              entry.incomingUID[0]
            } else {
              -1L
            }
          }
          prevMainEvent = load(incomingUID) as ProcessEvent
        } else {
          // If prevMainEvent is null we are at the beginning already!
          // Set the current event, so we can continue...
          prevMainEvent = event
        }
        // Retrieve all main events and get the highest row index
        val processes = getEventsOfProcess(prevMainEvent, onlyMainEvents = true)
        if (processes.path.isEmpty()) {
          // If for whatever reason there are no process events related...
          // ... to this one just set rowIndex to the default
          event.rowIndex = 20_0000
        } else {
          for (segment in processes.path) {
            try {
              if (segment.event.rowIndex > highestRowIndex) highestRowIndex = segment.event.rowIndex
            } catch (_: Exception) {
              // Entry could not get loaded!
            }
          }
          highestRowIndex += 20_000
          event.rowIndex = highestRowIndex
        }
      } else {
        // Alternate event
        // Retrieve all alternate events and get highest row index
        val tPrev = load(event.incomingUID[0]) as ProcessEvent
        for (i in 1 until tPrev.outgoingUID.size) {
          try {
            entry = load(tPrev.outgoingUID[i]) as ProcessEvent
            if (entry.rowIndex > highestRowIndex) highestRowIndex = entry.rowIndex
          } catch (_: Exception) {
            // Entry could not get loaded!
          }
        }
        highestRowIndex += 20_000
        event.rowIndex = highestRowIndex
      }
    }
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
    if (mode.contains("^row$".toRegex())) {
      // If mode was set to "row", update the rowIndex
      processEvent!!.rowIndex = config.rowIndex
      // Did this event change main process?
      if (config.previousEventGUID.isNotEmpty()) {
        var prevEvent: ProcessEvent? = null
        getEntriesFromIndexSearch(
                searchText = "^${config.previousEventGUID}$", ixNr = 1, showAll = true) {
          it as ProcessEvent
          prevEvent = it
        }
        if (prevEvent === null) {
          appCall.respond(HttpStatusCode.NotFound)
          return
        }
        if (!processEvent!!.incomingUID.contains(
                  prevEvent!!.uID)) { // || processEvent!!.incomingUID[0] != prevEvent!!.uID) {
          // Event does not contain provided previous event -> previous event changed!
          // ### STEP 1: Clean up current event's in and outgoing events
          freeProcessEvent(processEvent!!)
          // ### STEP 2: Now replace current event's main incoming and outgoing event to make it fit in again
          // a) Replace ingoing with new previous event
          processEvent!!.incomingUID[0] = prevEvent!!.uID
          // b) Replace outgoing with next event of new previous event
          /*
          if (config.nextEventGUID.isNotEmpty() && prevEvent!!.outgoingUID.isNotEmpty()) {
            val nextEvent = get(prevEvent!!.outgoingUID[0]) as ProcessEvent
            processEvent!!.outgoingUID[0] = nextEvent.uID
            nextEvent.incomingUID[0] = processEvent!!.uID
            saveEntry(nextEvent)
          }
           */
          // ### STEP 3: Clean up the new previous and next events to close the chain again
          prevEvent!!.outgoingUID.add(processEvent!!.uID)
          saveEntry(prevEvent!!)
        }
      } else if (config.nextEventGUID.isNotEmpty()) {
        // Check next event, too, to make sure the main events are a stable double linked list
        var nextEvent: ProcessEvent? = null
        getEntriesFromIndexSearch(
                searchText = "^${config.nextEventGUID}$", ixNr = 1, showAll = true) {
          it as ProcessEvent
          nextEvent = it
        }
        if (nextEvent === null) {
          appCall.respond(HttpStatusCode.NotFound)
          return
        }
        if (!processEvent!!.outgoingUID.contains(nextEvent!!.uID) || processEvent!!.outgoingUID[0] != nextEvent!!.uID) {
          // Event does not contain provided next event -> next event changed!
          // ### STEP 1: Clean up current event's in and outgoing events
          freeProcessEvent(processEvent!!)
          // ### STEP 2: Now replace current event's main incoming and outgoing event to make it fit in again
          // a) Replace ingoing with previous event of new next event
          if (nextEvent!!.incomingUID.isNotEmpty()) {
            val prevEvent = get(nextEvent!!.incomingUID[0]) as ProcessEvent
            processEvent!!.incomingUID[0] = prevEvent.uID
            prevEvent.outgoingUID[0] = processEvent!!.uID
            saveEntry(prevEvent)
          }
          // b) Replace outgoing with new next event
          processEvent!!.outgoingUID[0] = nextEvent!!.uID
          // ### STEP 3: Clean up the new previous and next events to close the chain again
          nextEvent!!.incomingUID[0] = processEvent!!.uID
          saveEntry(nextEvent!!)
        }
      }
      // Save and respond
      saveEntry(processEvent!!)
      appCall.respond(HttpStatusCode.OK)
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

  private suspend fun freeProcessEvent(processEvent: ProcessEvent) {
    // a) Remove current event's uID from original previous
    if (processEvent.incomingUID.isNotEmpty()) {
      val tPrevEvent = get(processEvent.incomingUID[0]) as ProcessEvent
      tPrevEvent.outgoingUID.remove(processEvent.uID)
      saveEntry(tPrevEvent)
    }
    // b) Remove current event's uID from original next
    if (processEvent.outgoingUID.isNotEmpty()) {
      val tNextEvent = get(processEvent.outgoingUID[0]) as ProcessEvent
      tNextEvent.incomingUID.remove(processEvent.uID)
      saveEntry(tNextEvent)
    }
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
    val pair: Pair<Regex, List<String>>
    var queryWordsResults: MutableMap<String, Long>? = null
    var regexPattern: Regex? = null
    var doQuery = false
    if (queryFilter.isNotEmpty()) {
      pair = buildQueryRegexPattern(queryFilter)
      regexPattern = pair.first
      queryWordsResults = getQueryWordsResultInit(pair.second)
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
                  regexPattern = regexPattern!!, queryWordsResults = queryWordsResults!!, entry = it, title = it.title,
                  keywords = it.keywords, description = it.description, authorUsername = it.authorUsername)

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
          if (tmpEntry.outgoingUID[0] == processEvent!!.uID && processEvent!!.outgoingUID.isNotEmpty()) {
            // Event was main event -> prepare to close gap
            tmpEntry.outgoingUID[0] = -1L
          } else {
            // Event was main event or there will not be a following event -> remove uID
            tmpEntry.outgoingUID.remove(processEvent!!.uID)
          }
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
            if (processEvent!!.incomingUID.isNotEmpty()) {
              tmpEntry.incomingUID.add(processEvent!!.incomingUID[0])
              // Close gap for previous event, too
              val tmpPrevEntry = get(processEvent!!.incomingUID[0]) as ProcessEvent
              if (tmpPrevEntry.outgoingUID[0] == -1L) {
                // Previous entry does not contain a following entry -> add as main
                tmpPrevEntry.outgoingUID[0] = tmpEntry.uID
              } else {
                // Previous entry already contains next ones -> add as alternate event
                tmpPrevEntry.outgoingUID.add(tmpEntry.uID)
              }
              saveEntry(tmpPrevEntry)
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
    var processes = getEventsOfProcess(entryPoint!!)
    if (processes.path.isNotEmpty()) {
      processes = sortSegments(processes)
      // With tasks being inserted everywhere, we need to care for the first task to receive a row index too small
      if (processes.path[0].event.rowIndex < 10) {
        processes = reSortSegments(processes)
      } else {
        // Quick check to make sure all tasks are in order by making sure the last task's row index is greater than zero
        if (processes.path[processes.path.size - 1].event.rowIndex <= 0) {
          processes = reSortSegments(processes)
        }
      }
    }
    appCall.respond(processes)
  }

  /**
   * Recursively retrieves all events following the specified [ProcessEvent].
   * @return [ProcessPathPayload]
   */
  private tailrec suspend fun getEventsOfProcess(
    processEvent: ProcessEvent,
    processes: ProcessPathPayload = ProcessPathPayload(),
    onlyMainEvents: Boolean = false
  ): ProcessPathPayload {
    var segment = ProcessPathFullSegmentPayload(processEvent)
    // Related entries?
    val wisdomController = WisdomController()
    var wisdom: Wisdom
    if (!onlyMainEvents && processEvent.wisdomUID != -1L) {
      try {
        wisdom = wisdomController.get(processEvent.wisdomUID) as Wisdom
        segment.wisdom = wisdom
      } catch (_: Exception) {
      }
    }
    if (!onlyMainEvents && processEvent.taskWisdomUID != -1L) {
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
    if (!onlyMainEvents && processEvent.outgoingUID.size > 1) {
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
      if (segment.alternatives.isNotEmpty()) {
        segment = sortSegmentAlternatives(segment)
        // With tasks being inserted everywhere, we need to care for the first task to receive a row index too small
        if (segment.alternatives[0].event.rowIndex < 10) {
          segment = reSortSegmentAlternatives(segment)
        } else {
          // Quick check to make sure all tasks are in order by making sure the last task's row index is greater than zero
          if (segment.alternatives[segment.alternatives.size - 1].event.rowIndex <= 0) {
            segment = reSortSegmentAlternatives(segment)
          }
        }
      }
    }
    processes.path.add(segment)
    return getEventsOfProcess(nextEvent, processes)
  }

  private suspend fun reSortSegmentAlternatives(
    segment: ProcessPathFullSegmentPayload
  ): ProcessPathFullSegmentPayload {
    var lastRowIndex = 20_000 * segment.alternatives.size
    var task: ProcessEvent
    for (j in 0 until segment.alternatives.size) {
      // Update task
      segment.alternatives[j].event.rowIndex = lastRowIndex
      // Update Wisdom entry
      task = get(segment.alternatives[j].event.uID) as ProcessEvent
      task.rowIndex = lastRowIndex
      saveEntry(task, false)
      // Decrement row index
      lastRowIndex -= 20_000
    }
    return segment
  }

  private fun sortSegmentAlternatives(
    segment: ProcessPathFullSegmentPayload
  ): ProcessPathFullSegmentPayload {
    val sortedTasks = segment.alternatives.sortedWith(compareBy { it.event.rowIndex })
    segment.alternatives.clear()
    for (j in sortedTasks.indices) {
      segment.alternatives.add(sortedTasks[j])
    }
    return segment
  }

  private suspend fun reSortSegments(
    path: ProcessPathPayload
  ): ProcessPathPayload {
    var lastRowIndex = 20_000 * path.path.size
    var task: ProcessEvent
    for (j in 0 until path.path.size) {
      // Update task
      path.path[j].event.rowIndex = lastRowIndex
      // Update Wisdom entry
      task = get(path.path[j].event.uID) as ProcessEvent
      task.rowIndex = lastRowIndex
      saveEntry(task, false)
      // Decrement row index
      lastRowIndex -= 20_000
    }
    return path
  }

  private fun sortSegments(
    path: ProcessPathPayload
  ): ProcessPathPayload {
    val sortedTasks = path.path.sortedWith(compareBy { it.event.rowIndex })
    path.path.clear()
    for (j in sortedTasks.indices) {
      path.path.add(sortedTasks[j])
    }
    return path
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
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall)) ?: return false
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
    var boxUID = -1L
    for ((index, segment) in path.path.withIndex()) {
      if (index == 0) {
        // Check if the box exists
        if (segment.event.taskWisdomUID == -1L) {
          // Box does not exist -> Create it
          box = Wisdom(-1)
          box.type = "box"
          box.knowledgeUID = knowledgeRef.uID
          box.authorUsername = user.username
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
          checkEventTask(segment.event, knowledgeRef, box, user, wisdomController)
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
            checkEventTask(segment.event, knowledgeRef, box, user, wisdomController)
          } catch (e: Exception) {
            appCall.respond(HttpStatusCode.BadRequest)
            return true
          }
        }
        if (segment.alternatives.isNotEmpty()) {
          for (altEvent in segment.alternatives) {
            checkEventTask(altEvent.event, knowledgeRef, box, user, wisdomController)
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
      checkEventTask(segment.event, knowledgeRef, box, user, wisdomController)
      if (segment.alternatives.isNotEmpty()) {
        for (altEvent in segment.alternatives) {
          checkEventTask(altEvent.event, knowledgeRef, box, user, wisdomController)
        }
      }
    }
    appCall.respond(box!!.guid)
    WisdomController().notifyKnowledgeMembers(
            knowledgeGUID = knowledgeRef.guid, message = "", srcUsername = user.username)
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
      // task.keywords = event.keywords
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
