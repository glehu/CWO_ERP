package modules.m7wisdom.logic

import api.logic.core.Connector
import api.logic.core.ServerController
import api.misc.json.CategoriesPayload
import api.misc.json.CategoryPayload
import api.misc.json.ConnectorFrame
import api.misc.json.KeywordsPayload
import api.misc.json.PlannerBoxTasksPayload
import api.misc.json.PlannerBoxesPayload
import api.misc.json.PlannerTaskPayload
import api.misc.json.UniMessageReaction
import api.misc.json.WisdomAnswerCreation
import api.misc.json.WisdomCollaboratorEditPayload
import api.misc.json.WisdomCommentCreation
import api.misc.json.WisdomHistoryEntry
import api.misc.json.WisdomLessonCreation
import api.misc.json.WisdomQuestionCreation
import api.misc.json.WisdomReferencesResponse
import api.misc.json.WisdomSearchQuery
import api.misc.json.WisdomSearchResponse
import api.misc.json.WisdomSearchResponseEntry
import api.misc.json.WisdomTopContributorsResponse
import api.misc.json.WisdomTopContributorsResponseEntry
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m2.Contact
import modules.m5.UniMember
import modules.m5.logic.UniChatroomController
import modules.m7knowledge.Knowledge
import modules.m7knowledge.logic.KnowledgeController
import modules.m7wisdom.Wisdom
import modules.m7wisdom.WisdomCategory
import modules.m7wisdom.WisdomCollaborator
import modules.m8notification.Notification
import modules.m8notification.logic.NotificationController
import modules.m9process.ProcessEvent
import modules.m9process.logic.ProcessController
import modules.mx.logic.Timestamp
import modules.mx.logic.UserCLIManager
import modules.mx.wisdomIndexManager
import kotlin.system.measureTimeMillis

@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
class WisdomController : IModule {
  override val moduleNameLong = "WisdomController"
  override val module = "M7WISDOM"
  override fun getIndexManager(): IIndexManager {
    return wisdomIndexManager!!
  }

  companion object {
    val mutex = Mutex()
  }

  suspend fun saveEntry(
    wisdom: Wisdom,
    indexToDisk: Boolean = true
  ): Long {
    var uID: Long
    mutex.withLock {
      uID = save(wisdom, indexWriteToDisk = indexToDisk)
    }
    return uID
  }

  fun getWisdomFromGUID(wisdomGUID: String): Wisdom? {
    var wisdom: Wisdom? = null
    getEntriesFromIndexSearch(
            searchText = "^$wisdomGUID$", ixNr = 1, showAll = true) {
      it as Wisdom
      wisdom = it
    }
    return wisdom
  }

  fun getKnowledgeFromGUID(knowledgeGUID: String): Knowledge? {
    var knowledgeRef: Knowledge? = null
    KnowledgeController().getEntriesFromIndexSearch(
            searchText = "^${knowledgeGUID}$", ixNr = 1, showAll = true) {
      it as Knowledge
      knowledgeRef = it
    }
    return knowledgeRef
  }

  suspend fun httpCreateQuestion(
    appCall: ApplicationCall,
    config: WisdomQuestionCreation,
    wisdomGUID: String = ""
  ) {
    if (config.knowledgeGUID.isEmpty()) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
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
    var edit = false
    var question = Wisdom()
    question.type = "question"
    if (wisdomGUID.isNotEmpty()) {
      // Get existing wisdom if one got referenced
      getEntriesFromIndexSearch(
              searchText = "^$wisdomGUID$", ixNr = 1, showAll = true) {
        it as Wisdom
        question = it
        edit = true
      }
      if (!edit) {
        appCall.respond(HttpStatusCode.NotFound)
        return
      } else {
        // Are we allowed to alter this entry?
        if (!httpCheckWisdomRights(appCall, question, false, user = user)) return
      }
    }
    // Meta
    question.knowledgeUID = knowledgeRef!!.uID
    if (question.authorUsername.isEmpty()) {
      question.authorUsername = user!!.username
    }
    // Info
    question.title = config.title
    question.description = config.description
    question.keywords = config.keywords
    question.categories = config.categories
    question.copyContent = config.copyContent
    saveEntry(question)
    appCall.respond(question.guid)
  }

  suspend fun httpCreateAnswer(
    appCall: ApplicationCall,
    config: WisdomAnswerCreation,
    wisdomGUID: String
  ) {
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
    var wisdomRef: Wisdom? = null
    getEntriesFromIndexSearch(
            searchText = "^${config.wisdomGUID}$", ixNr = 3, showAll = true) {
      it as Wisdom
      wisdomRef = it
    }
    if (wisdomRef == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    var edit = false
    var answer = Wisdom()
    answer.type = "answer"
    if (wisdomGUID.isNotEmpty()) {
      // Get existing wisdom if one got referenced
      getEntriesFromIndexSearch(
              searchText = "^$wisdomGUID$", ixNr = 1, showAll = true) {
        it as Wisdom
        answer = it
        edit = true
      }
      if (!edit) {
        appCall.respond(HttpStatusCode.NotFound)
        return
      } else {
        // Are we allowed to alter this entry?
        if (!httpCheckWisdomRights(appCall, answer, user = user)) return
      }
    }
    // Meta
    answer.knowledgeUID = wisdomRef!!.knowledgeUID
    answer.srcWisdomUID = wisdomRef!!.uID
    if (answer.authorUsername.isEmpty()) {
      answer.authorUsername = user!!.username
    }
    // Info
    answer.title = config.title
    answer.description = config.description
    answer.keywords = config.keywords
    answer.copyContent = config.copyContent
    saveEntry(answer)
    appCall.respond(answer.guid)
  }

  suspend fun httpCreateLesson(
    appCall: ApplicationCall,
    config: WisdomLessonCreation,
    wisdomGUID: String,
    mode: String
  ) {
    if (config.knowledgeGUID.isEmpty()) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
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
    var lesson = Wisdom()
    lesson.type = "lesson"
    var edit = false
    if (wisdomGUID.isNotEmpty()) {
      // Get existing wisdom if one got referenced
      getEntriesFromIndexSearch(
              searchText = "^$wisdomGUID$", ixNr = 1, showAll = true) {
        it as Wisdom
        lesson = it
        edit = true
      }
      if (!edit) {
        appCall.respond(HttpStatusCode.NotFound)
        return
      } else {
        // Are we allowed to alter this entry?
        if (!httpCheckWisdomRights(appCall, lesson, false, user = user)) return
      }
    }
    // If mode was set to "row", update the rowIndex only
    if (mode.contains("^row$".toRegex())) {
      lesson.rowIndex = config.rowIndex
      if (config.boxGUID.isNotEmpty()) {
        var boxWisdom: Wisdom? = null
        getEntriesFromIndexSearch(
                searchText = "^${config.boxGUID}$", ixNr = 1, showAll = true) {
          it as Wisdom
          boxWisdom = it
        }
        if (boxWisdom != null) {
          lesson.srcWisdomUID = boxWisdom!!.uID
          lesson.keywords = boxWisdom!!.title
        }
      }
      lesson.history.add(
              Json.encodeToString(
                      WisdomHistoryEntry(
                              type = "move", date = Timestamp.getUnixTimestampHex(), description = "Moved",
                              authorUsername = user!!.username)))
      saveEntry(lesson)
      appCall.respond(lesson.guid)
      if (lesson.isTask) notifyPlannerKnowledgeMembers(knowledgeRef!!.guid, "", user.username)
      return
    }
    // If mode was set to "due", update the due dates only and fix them while doing so
    if (mode.contains("^due$".toRegex())) {
      lesson.dueDate = config.dueDate
      lesson.dueDateUntil = config.dueDateUntil
      if (lesson.dueDate.isNotEmpty() || lesson.dueDateUntil.isNotEmpty()) {
        if (!lesson.hasDueDate) lesson.hasDueDate = true
        if (lesson.dueDate.isEmpty()) lesson.dueDate = lesson.dueDateUntil
        if (lesson.dueDateUntil.isEmpty()) lesson.dueDateUntil = lesson.dueDate
        if (lesson.finished) {
          lesson.finished = false
          lesson.finishedDate = ""
        }
      }
      lesson.history.add(
              Json.encodeToString(
                      WisdomHistoryEntry(
                              type = "edit", date = Timestamp.getUnixTimestampHex(), description = "Due Date Edited",
                              authorUsername = user!!.username)))
      saveEntry(lesson)
      appCall.respond(lesson.guid)
      if (lesson.isTask) notifyPlannerKnowledgeMembers(knowledgeRef!!.guid, "", user.username)
      return
    }
    // If mode was set to "edit", update some human-entered fields only.
    // This mode is useful since those fields are the only ones that can be edited in the WisdomViewer of wikiric
    if (mode.contains("^edit$".toRegex())) {
      lesson.title = config.title
      lesson.description = config.description
      lesson.keywords = config.keywords
      lesson.categories = config.categories
      lesson.copyContent = config.copyContent
      lesson.history.add(
              Json.encodeToString(
                      WisdomHistoryEntry(
                              type = "edit", date = Timestamp.getUnixTimestampHex(), description = "Edited",
                              authorUsername = user!!.username)))
      saveEntry(lesson)
      appCall.respond(lesson.guid)
      if (lesson.isTask) notifyPlannerKnowledgeMembers(knowledgeRef!!.guid, "", user.username)
      return
    }
    // Meta
    lesson.knowledgeUID = knowledgeRef!!.uID
    if (lesson.authorUsername.isEmpty()) {
      lesson.authorUsername = user!!.username
    }
    // Info
    lesson.title = config.title
    lesson.description = config.description
    lesson.keywords = config.keywords
    lesson.copyContent = config.copyContent
    lesson.categories = config.categories
    lesson.isTask = config.isTask
    lesson.taskType = config.taskType
    if (lesson.taskType.isEmpty()) {
      lesson.isTask = false
    }
    if (lesson.isTask || lesson.taskType.isNotEmpty()) {
      lesson.isTask = true
      lesson.type = config.taskType
      if (!edit) {
        // Add box's title to its keywords
        if (config.taskType.lowercase() == "box") {
          if (!config.keywords.contains(config.title)) {
            lesson.keywords += config.title
          }
        }
      }
    }
    lesson.columnIndex = config.columnIndex
    lesson.rowIndex = config.rowIndex
    lesson.hasDueDate = config.hasDueDate
    lesson.dueDate = config.dueDate
    lesson.dueDateUntil = config.dueDateUntil
    // Reference the Box containing this task if specified
    if (config.inBox && config.boxGUID.isNotEmpty()) {
      var boxWisdom: Wisdom? = null
      getEntriesFromIndexSearch(
              searchText = "^${config.boxGUID}$", ixNr = 1, showAll = true) {
        it as Wisdom
        boxWisdom = it
      }
      if (boxWisdom != null) {
        lesson.srcWisdomUID = boxWisdom!!.uID
        if (!edit) {
          // Add the box's title to the task's keywords
          if (!config.keywords.contains(boxWisdom!!.title)) {
            if (lesson.keywords.isNotEmpty()) lesson.keywords += ','
            lesson.keywords += boxWisdom!!.title
          }
        }
        // Check rowIndex
        if (lesson.rowIndex == 0) {
          var highestRowIndex = 0
          getEntriesFromIndexSearch(
                  searchText = "^${boxWisdom!!.uID}$", ixNr = 3, showAll = true) { taskIt ->
            taskIt as Wisdom
            // Filter if there is a preference for "is finished"
            if (!taskIt.finished) {
              if (taskIt.rowIndex > highestRowIndex) highestRowIndex = taskIt.rowIndex
            }
          }
          highestRowIndex += 20_000
          lesson.rowIndex = highestRowIndex
        }
      } else {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
    }
    if (!edit) {
      lesson.history.add(
              Json.encodeToString(
                      WisdomHistoryEntry(
                              type = "creation", date = Timestamp.getUnixTimestampHex(), description = "Created",
                              authorUsername = user!!.username)))
    } else {
      lesson.history.add(
              Json.encodeToString(
                      WisdomHistoryEntry(
                              type = "edit", date = Timestamp.getUnixTimestampHex(), description = "Edited",
                              authorUsername = user!!.username)))
    }
    saveEntry(lesson)
    appCall.respond(lesson.guid)
    if (lesson.isTask) notifyPlannerKnowledgeMembers(knowledgeRef!!.guid, "", user.username)
  }

  suspend fun httpCreateComment(
    appCall: ApplicationCall,
    config: WisdomCommentCreation,
    wisdomGUID: String
  ) {
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
    var wisdomRef: Wisdom? = null
    getEntriesFromIndexSearch(
            searchText = "^${config.wisdomGUID}$", ixNr = 1, showAll = true) {
      it as Wisdom
      wisdomRef = it
    }
    if (wisdomRef == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    var edit = false
    var comment = Wisdom()
    comment.type = "comment"
    if (wisdomGUID.isNotEmpty()) {
      // Get existing wisdom if one got referenced
      getEntriesFromIndexSearch(
              searchText = "^$wisdomGUID$", ixNr = 1, showAll = true) {
        it as Wisdom
        comment = it
        edit = true
      }
      if (!edit) {
        appCall.respond(HttpStatusCode.NotFound)
        return
      } else {
        // Are we allowed to alter this entry?
        if (!httpCheckWisdomRights(appCall, comment, user = user)) return
      }
    }
    // Meta
    comment.knowledgeUID = wisdomRef!!.knowledgeUID
    comment.srcWisdomUID = wisdomRef!!.uID
    if (comment.authorUsername.isEmpty()) {
      comment.authorUsername = user!!.username
    }
    // Info
    comment.title = config.title
    comment.description = config.description
    comment.keywords = config.keywords
    saveEntry(comment)
    appCall.respond(comment.guid)
    if (!edit) {
      if (wisdomRef!!.authorUsername != user!!.username) {
        val notification = Notification(-1, wisdomRef!!.authorUsername)
        notification.title = "${user.username} commented!"
        notification.authorUsername = "_server"
        if (wisdomRef!!.type != "comment") {
          notification.description = "${user.username} has commented \"${wisdomRef!!.title.trim()}\""
        } else {
          notification.description = "${user.username} has replied to one of your comments!"
        }
        notification.hasClickAction = true
        notification.clickAction = "open,wisdom"
        notification.clickActionReferenceGUID = wisdomRef!!.guid
        NotificationController().saveEntry(notification)
        Connector.sendFrame(
                username = wisdomRef!!.authorUsername, frame = ConnectorFrame(
                type = "notification", msg = notification.description, date = Timestamp.now(),
                obj = Json.encodeToString(notification), srcUsername = user.username, wisdomGUID = wisdomRef!!.guid))
      }
      if (wisdomRef!!.isTask) {
        val knowledgeRef = KnowledgeController().load(wisdomRef!!.knowledgeUID) as Knowledge
        notifyPlannerKnowledgeMembers(knowledgeRef.guid, "", user.username)
      }
    }
  }

  suspend fun httpWisdomQuery(
    appCall: ApplicationCall,
    config: WisdomSearchQuery,
    knowledgeGUID: String?
  ) {
    // val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
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
    // Matches Total
    var matchesAll: Sequence<MatchResult>
    var matchDestructuredList: List<String>
    // Etc
    var rating: Int
    var accuracyTotalCount: Int
    var foundTmp: Int
    var accuracyPercentage: Double
    val first: ArrayList<WisdomSearchResponseEntry> = arrayListOf()
    val second: ArrayList<WisdomSearchResponseEntry> = arrayListOf()
    val third: ArrayList<WisdomSearchResponseEntry> = arrayListOf()
    val elapsedMillis = measureTimeMillis {
      // Split words on each whitespace after removing duplicate whitespaces
      val cleanQuery = config.query.replace("\\s+".toRegex()) { it.value[0].toString() }
      val queryWords = cleanQuery.split("\\s".toRegex())
      val queryWordsResults = getQueryWordsResultInit(queryWords)
      val queryFormatted = StringBuilder()
      for ((amount, word) in queryWords.withIndex()) {
        if (amount > 0) queryFormatted.append("|")
        queryFormatted.append("(")
        queryFormatted.append(word)
        // Negative Lookahead to prevent tokens to be matched multiple times
        queryFormatted.append("(?!.*")
        queryFormatted.append(word)
        queryFormatted.append("))")
      }
      val queryFormattedString = queryFormatted.toString()
      val regexPattern = queryFormattedString.toRegex(RegexOption.IGNORE_CASE)
      val indexNumber: Int
      val indexQuery: String
      when (config.type) {
        "wisdom" -> {
          indexNumber = 2
          indexQuery = "^${knowledgeRef!!.uID}$"
        }

        "task" -> {
          indexNumber = 6
          indexQuery = "^${knowledgeRef!!.uID};.*$"
        }

        else -> {
          indexNumber = 2
          indexQuery = "^${knowledgeRef!!.uID}$"
        }
      }
      var regexMatchCounts: Int
      getEntriesFromIndexSearch(
              searchText = indexQuery, ixNr = indexNumber, showAll = true) {
        it as Wisdom
        var valid = true
        // If type of wisdom differs from preference then invalidate it
        if (config.entryType.isNotEmpty() && it.type != config.entryType) valid = false
        if (valid && config.state.isNotEmpty()) {
          // If state of wisdom differs from preference then invalidate it
          when (config.state) {
            "true" -> if (!it.finished) valid = false
            "false" -> if (it.finished) valid = false
          }
        }
        if (valid) {
          rating = 0
          accuracyTotalCount = 0
          // Title
          if (config.filterOverride.isEmpty() || config.filterOverride.contains("title")) {
            matchesAll = regexPattern.findAll(it.title)
            regexMatchCounts = matchesAll.count()
            if (regexMatchCounts > 0) {
              rating += 2
              accuracyTotalCount += regexMatchCounts
              for (match in matchesAll) {
                matchDestructuredList = match.destructured.toList()
                for (matchedWord in matchDestructuredList) {
                  if (matchedWord.isNotEmpty()) {
                    queryWordsResults[matchedWord] = it.uID
                  }
                }
              }
            }
          }
          // Keywords
          if (config.filterOverride.isEmpty() || config.filterOverride.contains("keywords")) {
            matchesAll = regexPattern.findAll(it.keywords)
            regexMatchCounts = matchesAll.count()
            if (regexMatchCounts > 0) {
              rating += 2
              accuracyTotalCount += regexMatchCounts
              for (match in matchesAll) {
                matchDestructuredList = match.destructured.toList()
                for (matchedWord in matchDestructuredList) {
                  if (matchedWord.isNotEmpty()) {
                    queryWordsResults[matchedWord] = it.uID
                  }
                }
              }
            }
          }
          // Description
          if (config.filterOverride.isEmpty() || config.filterOverride.contains("description")) {
            matchesAll = regexPattern.findAll(it.description)
            regexMatchCounts = matchesAll.count()
            if (regexMatchCounts > 0) {
              rating += 1
              accuracyTotalCount += regexMatchCounts
              for (match in matchesAll) {
                matchDestructuredList = match.destructured.toList()
                for (matchedWord in matchDestructuredList) {
                  if (matchedWord.isNotEmpty()) {
                    queryWordsResults[matchedWord] = it.uID
                  }
                }
              }
            }
          }
          // Author
          if (config.filterOverride.isEmpty() || config.filterOverride.contains("author")) {
            matchesAll = regexPattern.findAll(it.authorUsername)
            regexMatchCounts = matchesAll.count()
            if (regexMatchCounts > 0) {
              rating += 1
              accuracyTotalCount += regexMatchCounts
              for (match in matchesAll) {
                matchDestructuredList = match.destructured.toList()
                for (matchedWord in matchDestructuredList) {
                  if (matchedWord.isNotEmpty()) {
                    queryWordsResults[matchedWord] = it.uID
                  }
                }
              }
            }
          }
          // Discard if irrelevant
          if (rating > 0) {
            // Cut the description to a maximum of 200 characters if there is one
            if (it.description.isNotEmpty()) {
              var description = it.description
              var truncated = false
              if (it.description.length > 200) {
                description = description.substring(0..200)
                truncated = true
              }
              // Remove line breaks as this could lead to broken design
              description = description.replace("(\r\n|\r|\n)".toRegex(), " ")
              // Remove mermaid markdown graphs since they can only exist as a whole, which might be too much to show
              description = description.replace(regex = """```.*(```)?""".toRegex(), replacement = "")
              if (truncated) description = "$description..."
              it.description = description
            }
            // Clean up dates (human-readable)
            if (it.finished) {
              it.finishedDate = Timestamp.getUTCTimestampFromHex(it.finishedDate)
            }
            it.dateCreated = Timestamp.getUTCTimestampFromHex(it.dateCreated)
            // Calculate accuracy percentage
            foundTmp = 0
            for (queryWordResult in queryWordsResults) {
              if (queryWordResult.value == it.uID) {
                foundTmp += 1
              }
            }
            accuracyPercentage = foundTmp.toDouble() / queryWordsResults.size.toDouble()
            // Evaluate
            if (rating >= 4) {
              first.add(
                      WisdomSearchResponseEntry(it, accuracyTotalCount, accuracyPercentage))
            } else if (rating >= 3) {
              second.add(
                      WisdomSearchResponseEntry(it, accuracyTotalCount, accuracyPercentage))
            } else {
              third.add(
                      WisdomSearchResponseEntry(it, accuracyTotalCount, accuracyPercentage))
            }
          }
        }
      }
    }
    if (first.isEmpty() && second.isEmpty() && third.isEmpty()) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    val response = WisdomSearchResponse()
    response.time = elapsedMillis.toInt()
    response.first = first.sortedWith(compareBy({ -it.accuracyPercent }, { -it.accuracyTotalCount }))
    response.second = second.sortedWith(compareBy({ -it.accuracyPercent }, { -it.accuracyTotalCount }))
    response.third = third.sortedWith(compareBy({ -it.accuracyPercent }, { -it.accuracyTotalCount }))
    val jsonPayload = Json.encodeToString(response)
    appCall.respond(jsonPayload)
  }

  suspend fun httpWisdomReact(
    appCall: ApplicationCall,
    config: UniMessageReaction,
    wisdomGUID: String?
  ) {
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
    if (user == null) {
      appCall.respond(HttpStatusCode.Unauthorized)
      return
    }
    var wisdom: Wisdom? = null
    getEntriesFromIndexSearch(
            searchText = "^$wisdomGUID$", ixNr = 1, showAll = true) {
      it as Wisdom
      wisdom = it
    }
    if (wisdom == null) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    var index = 0
    var reactionTypeExists = false
    var reactionAdded = false
    for (reaction in wisdom!!.reactions) {
      val react: UniMessageReaction
      // Self-healing! If a reaction is broken, just clear it
      try {
        react = Json.decodeFromString(reaction)
      } catch (e: Exception) {
        wisdom!!.reactions[index] = ""
        continue
      }
      if (react.type == config.type) {
        reactionTypeExists = true
        if (react.from.contains(user.username)) {
          // Reaction does exist and contains the user -> Remove
          react.from.remove(user.username)
          if (react.from.size > 0) {
            wisdom!!.reactions[index] = Json.encodeToString(react)
          } else {
            wisdom!!.reactions.removeAt(index)
          }
          break
        } else {
          // Reaction does exist but does not contain the user -> Append
          react.from.add(user.username)
          wisdom!!.reactions[index] = Json.encodeToString(react)
          reactionAdded = true
          break
        }
      }
      index++
    }
    if (!reactionTypeExists) {
      // Reaction does not exist -> Add
      val reaction = UniMessageReaction(
              from = arrayListOf(user.username), type = config.type)
      wisdom!!.reactions.add(Json.encodeToString(reaction))
      reactionAdded = true
    }
    save(wisdom!!)
    appCall.respond(HttpStatusCode.OK)
    if (reactionAdded) {
      if (wisdom!!.authorUsername != user.username) {
        val notification = Notification(-1, wisdom!!.authorUsername)
        notification.title = "${user.username} reacted!"
        notification.authorUsername = "_server"
        if (wisdom!!.type != "comment") {
          notification.description = "${user.username} has reacted to \"${wisdom!!.title}\""
        } else {
          notification.description = "${user.username} has reacted to one of your comments!"
        }
        notification.hasClickAction = true
        notification.clickAction = "open,wisdom"
        notification.clickActionReferenceGUID = wisdom!!.guid
        NotificationController().saveEntry(notification)
        Connector.sendFrame(
                username = wisdom!!.authorUsername, frame = ConnectorFrame(
                type = "notification", msg = notification.description, date = Timestamp.now(),
                obj = Json.encodeToString(notification), wisdomGUID = wisdom!!.guid))
      }
    }
  }

  suspend fun httpGetWisdomEntriesRelated(
    appCall: ApplicationCall,
    wisdomGUID: String?,
    type: String = "guid"
  ) {
    if (wisdomGUID == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    var wisdomRef: Wisdom? = null
    if (type == "guid") {
      getEntriesFromIndexSearch(
              searchText = "^$wisdomGUID$", ixNr = 1, showAll = true) {
        it as Wisdom
        wisdomRef = it
      }
    } else if (type == "uid") {
      wisdomRef = load(wisdomGUID.toLong()) as Wisdom
    }
    if (wisdomRef == null) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    // Retrieve and check knowledge
    if (wisdomRef!!.knowledgeUID != -1L) {
      val knowledgeController = KnowledgeController()
      val knowledgeRef: Knowledge
      try {
        knowledgeRef = knowledgeController.load(wisdomRef!!.knowledgeUID) as Knowledge
      } catch (e: Exception) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      if (!knowledgeController.httpCanAccessKnowledge(appCall, knowledgeRef)) {
        return
      }
    }
    val response = WisdomReferencesResponse(wisdomRef!!)
    // Related Wisdom entries
    getEntriesFromIndexSearch(
            searchText = "^${wisdomRef!!.uID}$", ixNr = 3, showAll = true) {
      it as Wisdom
      // Convert dates for convenience
      if (it.finished) {
        it.finishedDate = Timestamp.getUTCTimestampFromHex(it.finishedDate)
      }
      it.dateCreated = Timestamp.getUTCTimestampFromHex(it.dateCreated)
      when (it.type) {
        "answer" -> response.answers.add(it)
        "comment" -> response.comments.add(it)
        "task" -> response.tasks.add(it)
        "question" -> response.questions.add(it)
        "lesson" -> response.lessons.add(it)
      }
    }
    // Related Processes entries
    ProcessController().getEntriesFromIndexSearch("^${wisdomRef!!.uID}$", ixNr = 3, true) {
      it as ProcessEvent
      response.processes.add(it)
    }
    ProcessController().getEntriesFromIndexSearch("^${wisdomRef!!.uID}$", ixNr = 4, true) {
      it as ProcessEvent
      response.processes.add(it)
    }

    appCall.respond(response)
  }

  suspend fun httpWisdomTopContributors(
    appCall: ApplicationCall,
    knowledgeGUID: String?
  ) {
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
    val contributors = mutableMapOf<String, Int>()
    getEntriesFromIndexSearch(
            searchText = "^${knowledgeRef!!.uID}$", ixNr = 2, showAll = true) {
      it as Wisdom
      if (!it.isTask) {
        if (contributors.containsKey(it.authorUsername)) {
          contributors[it.authorUsername] = contributors[it.authorUsername]!! + 1
        } else {
          contributors[it.authorUsername] = 1
        }
      }
    }
    val contributorsSorted = contributors.toList().sortedBy { (_, value) -> value }.reversed().toMap()
    contributors.clear()
    val response = WisdomTopContributorsResponse()
    for (contributor in contributorsSorted) {
      response.contributors.add(
              WisdomTopContributorsResponseEntry(
                      username = contributor.key, imageURL = "", lessons = contributor.value))
    }
    appCall.respond(response)
  }

  suspend fun httpDeleteWisdom(
    appCall: ApplicationCall,
    wisdomGUID: String?
  ) {
    var wisdom: Wisdom? = null
    getEntriesFromIndexSearch(
            searchText = "^$wisdomGUID$", ixNr = 1, showAll = true) {
      it as Wisdom
      wisdom = it
    }
    if (wisdom == null) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    if (!httpCheckWisdomRights(appCall, wisdom!!)) return
    val isTask = wisdom!!.isTask
    val knowledgeUID = wisdom!!.knowledgeUID
    // Configure wisdom
    wisdom!!.guid = ""
    wisdom!!.knowledgeUID = -1
    wisdom!!.title = ""
    wisdom!!.description = ""
    wisdom!!.srcWisdomUID = -1L
    wisdom!!.refWisdomUID = -1L
    wisdom!!.isTask = false
    saveEntry(wisdom!!)
    appCall.respond(HttpStatusCode.OK)
    if (isTask) {
      val knowledgeRef = KnowledgeController().load(knowledgeUID) as Knowledge
      notifyPlannerKnowledgeMembers(
              knowledgeGUID = knowledgeRef.guid, message = "",
              srcUsername = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))!!.username)
    }
  }

  suspend fun httpGetWisdomEntry(
    appCall: ApplicationCall,
    wisdomGUID: String
  ) {
    var wisdom: Wisdom? = null
    getEntriesFromIndexSearch(
            searchText = "^$wisdomGUID$", ixNr = 1, showAll = true) {
      it as Wisdom
      if (it.finished) {
        it.finishedDate = Timestamp.getUTCTimestampFromHex(it.finishedDate)
      }
      it.dateCreated = Timestamp.getUTCTimestampFromHex(it.dateCreated)
      wisdom = it
    }
    if (wisdom == null) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    if (!httpCheckWisdomRights(appCall, wisdom!!, checkCollaborator = false)) return
    appCall.respond(wisdom!!)
  }

  suspend fun httpGetTasks(
    appCall: ApplicationCall,
    knowledgeGUID: String?,
    stateFilter: String
  ) {
    if (knowledgeGUID == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
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
    // First, fetch all boxes
    var plannerBoxesPayload = PlannerBoxesPayload()
    getEntriesFromIndexSearch(
            searchText = "^${knowledgeRef!!.uID};BOX$", ixNr = 6, showAll = true) {
      it as Wisdom
      if (it.finished) {
        it.finishedDate = Timestamp.getUTCTimestampFromHex(it.finishedDate)
      }
      it.dateCreated = Timestamp.getUTCTimestampFromHex(it.dateCreated)
      plannerBoxesPayload.boxes.add(PlannerBoxTasksPayload(it))
    }
    if (plannerBoxesPayload.boxes.isEmpty()) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    // Now, get the tasks of all gathered boxes
    var isFinishedDesire: Boolean? = false
    when (stateFilter) {
      "finished" -> isFinishedDesire = true
      "any", "all" -> isFinishedDesire = null
    }
    for (i in 0 until plannerBoxesPayload.boxes.size) {
      getEntriesFromIndexSearch(
              searchText = "^${plannerBoxesPayload.boxes[i].box.uID}$", ixNr = 3, showAll = true) { taskIt ->
        taskIt as Wisdom
        // Filter if there is a preference for "is finished"
        if (isFinishedDesire == null || taskIt.finished == isFinishedDesire) {
          val plannerTaskPayload = PlannerTaskPayload(taskIt)
          // Convert dates before sending the entries back
          if (taskIt.finished) {
            taskIt.finishedDate = Timestamp.getUTCTimestampFromHex(taskIt.finishedDate)
          }
          taskIt.dateCreated = Timestamp.getUTCTimestampFromHex(taskIt.dateCreated)
          // Retrieve comments for this task
          getEntriesFromIndexSearch(
                  searchText = "^${taskIt.uID}$", ixNr = 3, showAll = true) {
            it as Wisdom
            when (it.type) {
              "comment" -> {
                plannerTaskPayload.amountComments += 1
                if (plannerTaskPayload.recentComment == null) {
                  // Convert dates for convenience
                  if (it.finished) {
                    it.finishedDate = Timestamp.getUTCTimestampFromHex(it.finishedDate)
                  }
                  it.dateCreated = Timestamp.getUTCTimestampFromHex(it.dateCreated)
                  plannerTaskPayload.recentComment = it
                }
              }
            }
          }
          plannerBoxesPayload.boxes[i].tasks.add(plannerTaskPayload)
        }
      }
      if (plannerBoxesPayload.boxes[i].tasks.isNotEmpty()) {
        plannerBoxesPayload = sortPlannerBoxesPayload(plannerBoxesPayload, i)
        // With tasks being inserted everywhere, we need to care for the first task to receive a row index too small
        if (plannerBoxesPayload.boxes[i].tasks[0].task.rowIndex < 10) {
          plannerBoxesPayload = reSortPlannerBoxesPayload(plannerBoxesPayload, i)
        } else {
          // Quick check to make sure all tasks are in order by making sure the last task's row index is greater than zero
          if (plannerBoxesPayload.boxes[i].tasks[plannerBoxesPayload.boxes[i].tasks.size - 1].task.rowIndex <= 0) {
            plannerBoxesPayload = reSortPlannerBoxesPayload(plannerBoxesPayload, i)
          }
        }
      }
    }
    // Respond
    appCall.respond(plannerBoxesPayload)
  }

  private suspend fun reSortPlannerBoxesPayload(
    plannerBoxesPayload: PlannerBoxesPayload,
    i: Int
  ): PlannerBoxesPayload {
    var lastRowIndex = 20_000 * plannerBoxesPayload.boxes[i].tasks.size
    var task: Wisdom
    for (j in 0 until plannerBoxesPayload.boxes[i].tasks.size) {
      // Update task
      plannerBoxesPayload.boxes[i].tasks[j].task.rowIndex = lastRowIndex
      // Update Wisdom entry
      task = get(plannerBoxesPayload.boxes[i].tasks[j].task.uID) as Wisdom
      task.rowIndex = lastRowIndex
      saveEntry(task, false)
      // Decrement row index
      lastRowIndex -= 20_000
    }
    return sortPlannerBoxesPayload(plannerBoxesPayload, i)
  }

  private fun sortPlannerBoxesPayload(
    plannerBoxesPayload: PlannerBoxesPayload,
    i: Int
  ): PlannerBoxesPayload {
    val sortedTasks = plannerBoxesPayload.boxes[i].tasks.sortedWith(compareBy { it.task.rowIndex })
    plannerBoxesPayload.boxes[i].tasks.clear()
    for (j in sortedTasks.indices) {
      plannerBoxesPayload.boxes[i].tasks.add(sortedTasks[j])
    }
    return plannerBoxesPayload
  }

  suspend fun httpFinishWisdom(
    appCall: ApplicationCall,
    wisdomGUID: String?,
    answerGUID: String?
  ) {
    var wisdom: Wisdom? = null
    getEntriesFromIndexSearch(
            searchText = "^$wisdomGUID$", ixNr = 1, showAll = true) {
      it as Wisdom
      wisdom = it
    }
    if (wisdom == null) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
    if (!httpCheckWisdomRights(appCall, wisdom!!, user = user)) return
    // Configure wisdom
    wisdom!!.hasDueDate = false
    wisdom!!.finished = true
    wisdom!!.finishedDate = Timestamp.getUnixTimestampHex()
    // Do we need to mark a comment as the answer?
    if (!answerGUID.isNullOrEmpty()) {
      var answer: Wisdom? = null
      getEntriesFromIndexSearch(
              searchText = "^$answerGUID$", ixNr = 1, showAll = true) {
        it as Wisdom
        answer = it
      }
      if (answer == null) {
        appCall.respond(HttpStatusCode.NotFound)
        return
      }
      answer!!.type = "answer"
      // If the answered wisdom contains keywords, copy them to the answer, so it can be found more easily!
      if (wisdom!!.keywords.isNotEmpty()) {
        if (answer!!.keywords.isNotEmpty()) answer!!.keywords += ','
        answer!!.keywords += wisdom!!.keywords
      }
      saveEntry(answer!!)
    }
    // Write history entry
    val historyEntry = WisdomHistoryEntry(
            type = "completion", date = Timestamp.getUnixTimestampHex(), description = "Finished",
            authorUsername = user!!.username)
    wisdom!!.history.add(Json.encodeToString(historyEntry))
    saveEntry(wisdom!!)
    appCall.respond(HttpStatusCode.OK)
    if (wisdom!!.isTask) {
      val knowledgeRef = KnowledgeController().load(wisdom!!.knowledgeUID) as Knowledge
      notifyPlannerKnowledgeMembers(knowledgeRef.guid, "", user.username)
    }
  }

  private suspend fun httpCheckWisdomRights(
    appCall: ApplicationCall,
    wisdom: Wisdom,
    checkKnowledge: Boolean = true,
    checkCollaborator: Boolean = true,
    user: Contact? = null
  ): Boolean {
    if (checkKnowledge) {
      if (wisdom.knowledgeUID != -1L) {
        val knowledgeController = KnowledgeController()
        val knowledgeRef: Knowledge
        try {
          knowledgeRef = knowledgeController.load(wisdom.knowledgeUID) as Knowledge
        } catch (e: Exception) {
          appCall.respond(HttpStatusCode.BadRequest)
          return false
        }
        if (!knowledgeController.httpCanAccessKnowledge(appCall, knowledgeRef)) {
          return false
        }
      }
    }
    if (checkCollaborator) {
      // Retrieve user if not provided
      val userTmp = user ?: UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
      // If the user is unauthorized or neither the creator nor a collaborator, exit
      if (userTmp == null || (wisdom.authorUsername != userTmp.username && !wisdom.isCollaborator(userTmp.username))) {
        appCall.respond(HttpStatusCode.Forbidden)
        return false
      }
    }
    return true
  }

  private fun Wisdom.isCollaborator(
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

  suspend fun httpModifyWisdomContributor(
    appCall: ApplicationCall,
    wisdomGUID: String?,
    config: WisdomCollaboratorEditPayload
  ) {
    var wisdom: Wisdom? = null
    getEntriesFromIndexSearch(
            searchText = "^$wisdomGUID$", ixNr = 1, showAll = true) {
      it as Wisdom
      wisdom = it
    }
    if (wisdom == null) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    if (!httpCheckWisdomRights(appCall, wisdom!!)) return
    for (collaborator in config.collaborators) {
      // Add collaborator if he doesn't exist yet
      if (!wisdom!!.isCollaborator(collaborator.username)) {
        if (collaborator.add) {
          wisdom!!.collaborators.add(Json.encodeToString(WisdomCollaborator(collaborator.username)))
        }
      } else if (!collaborator.add) {
        // Remove him if he does
        wisdom!!.isCollaborator(collaborator.username, true)
      }
    }
    save(wisdom!!)
    appCall.respond(HttpStatusCode.OK)
    if (wisdom!!.isTask) {
      val knowledgeRef = KnowledgeController().load(wisdom!!.knowledgeUID) as Knowledge
      val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
      notifyPlannerKnowledgeMembers(knowledgeRef.guid, "", user!!.username)
    }
  }

  suspend fun httpGetRecentKeywords(
    appCall: ApplicationCall,
    knowledgeGUID: String?
  ) {
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
    val indexNumber = 2
    val indexQuery = "^${knowledgeRef!!.uID}$"
    val keywords = arrayListOf<String>()
    getEntriesFromIndexSearch(
            searchText = indexQuery, ixNr = indexNumber, showAll = true) {
      it as Wisdom
      for (keyword in it.keywords.split(',')) {
        if (keyword.isNotEmpty()) keywords.add(keyword)
      }
    }
    val payload = KeywordsPayload()
    val maxCounter = 1000
    for ((counter, keyword) in keywords.reversed().withIndex()) {
      if (counter >= maxCounter) break
      payload.keywords.add(keyword)
    }
    appCall.respond(payload)
  }

  suspend fun httpGetRecentCategories(
    appCall: ApplicationCall,
    knowledgeGUID: String?
  ) {
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
    val indexNumber = 2
    val indexQuery = "^${knowledgeRef!!.uID}$"
    val payload = CategoriesPayload()
    var categoryDecoded: WisdomCategory
    var foundTmp: Boolean
    val json = Json {
      isLenient = true
      ignoreUnknownKeys = true
    }
    getEntriesFromIndexSearch(
            searchText = indexQuery, ixNr = indexNumber, showAll = true) {
      it as Wisdom
      for (category in it.categories) {
        categoryDecoded = json.decodeFromString(category)
        foundTmp = false
        if (payload.categories.isNotEmpty()) {
          for (cat in payload.categories) {
            if (cat.category == categoryDecoded.category) {
              cat.count++
              foundTmp = true
            }
          }
        }
        if (!foundTmp) payload.categories.add(CategoryPayload(categoryDecoded.category, 1))
      }
    }
    appCall.respond(payload)
  }

  suspend fun notifyPlannerKnowledgeMembers(
    knowledgeGUID: String,
    message: String,
    srcUsername: String
  ) {
    var knowledgeRef: Knowledge? = null
    KnowledgeController().getEntriesFromIndexSearch(
            searchText = "^$knowledgeGUID$", ixNr = 1, showAll = true) {
      it as Knowledge
      knowledgeRef = it
    }
    if (knowledgeRef == null) return
    if (knowledgeRef!!.mainChatroomGUID.isEmpty()) return
    val chatroom = UniChatroomController().getChatroom(knowledgeRef!!.mainChatroomGUID) ?: return
    var username: String
    chatroom.members.forEach {
      username = Json.decodeFromString<UniMember>(it).username
      if (username != srcUsername) {
        Connector.sendFrame(
                username = username, frame = ConnectorFrame(
                type = "planner change", msg = message, date = Timestamp.now(), srcUsername = srcUsername))
      }
    }
  }
}
