package modules.m7wisdom.logic

import api.logic.core.ServerController
import api.misc.json.TaskBoxPayload
import api.misc.json.TaskBoxesResponse
import api.misc.json.UniMessageReaction
import api.misc.json.WisdomAnswerCreation
import api.misc.json.WisdomCollaboratorPayload
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m7knowledge.Knowledge
import modules.m7knowledge.logic.KnowledgeController
import modules.m7wisdom.Wisdom
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

  private suspend fun saveEntry(wisdom: Wisdom): Int {
    var uID: Int
    mutex.withLock {
      uID = save(wisdom)
    }
    return uID
  }

  suspend fun httpCreateQuestion(
    appCall: ApplicationCall, config: WisdomQuestionCreation, wisdomGUID: String = ""
  ) {
    if (config.knowledgeGUID.isEmpty()) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
    var knowledgeRef: Knowledge? = null
    KnowledgeController().getEntriesFromIndexSearch(
      searchText = "^${config.knowledgeGUID}$", ixNr = 1, showAll = true
    ) {
      it as Knowledge
      knowledgeRef = it
    }
    if (knowledgeRef == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    var question = Wisdom()
    if (wisdomGUID.isNotEmpty()) {
      // Get existing wisdom if one got referenced
      getEntriesFromIndexSearch(
        searchText = "^$wisdomGUID$", ixNr = 1, showAll = true
      ) {
        it as Wisdom
        question = it
      }
    }
    // Meta
    question.type = "question"
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
    appCall.respond(question.gUID)
  }

  suspend fun httpCreateAnswer(
    appCall: ApplicationCall, config: WisdomAnswerCreation, wisdomGUID: String
  ) {
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
    var wisdomRef: Wisdom? = null
    getEntriesFromIndexSearch(
      searchText = "^${config.wisdomGUID}$", ixNr = 3, showAll = true
    ) {
      it as Wisdom
      wisdomRef = it
    }
    if (wisdomRef == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    var answer = Wisdom()
    if (wisdomGUID.isNotEmpty()) {
      // Get existing wisdom if one got referenced
      getEntriesFromIndexSearch(
        searchText = "^$wisdomGUID$", ixNr = 1, showAll = true
      ) {
        it as Wisdom
        answer = it
      }
    }
    // Meta
    answer.type = "answer"
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
    appCall.respond(answer.gUID)
  }

  suspend fun httpCreateLesson(
    appCall: ApplicationCall, config: WisdomLessonCreation, wisdomGUID: String
  ) {
    if (config.knowledgeGUID.isEmpty()) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
    var knowledgeRef: Knowledge? = null
    KnowledgeController().getEntriesFromIndexSearch(
      searchText = "^${config.knowledgeGUID}$", ixNr = 1, showAll = true
    ) {
      it as Knowledge
      knowledgeRef = it
    }
    if (knowledgeRef == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    var lesson = Wisdom()
    var edit = false
    if (wisdomGUID.isNotEmpty()) {
      // Get existing wisdom if one got referenced
      getEntriesFromIndexSearch(
        searchText = "^$wisdomGUID$", ixNr = 1, showAll = true
      ) {
        it as Wisdom
        lesson = it
        edit = true
      }
    }
    // Meta
    lesson.type = "lesson"
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
    if (lesson.isTask) {
      if (lesson.taskType.isNotEmpty()) {
        lesson.isTask = true
        lesson.type = config.taskType
      }
    }
    lesson.columnIndex = config.columnIndex
    lesson.rowIndex = config.rowIndex
    lesson.hasDueDate = config.hasDueDate
    lesson.dueDate = config.dueDate
    if (config.inBox && config.boxGUID.isNotEmpty()) {
      var boxWisdom: Wisdom? = null
      getEntriesFromIndexSearch(
        searchText = "^${config.boxGUID}$", ixNr = 1, showAll = true
      ) {
        it as Wisdom
        boxWisdom = it
      }
      if (boxWisdom != null) {
        lesson.srcWisdomUID = boxWisdom!!.uID
      } else {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
    }
    val historyEntry = if (!edit) {
      WisdomHistoryEntry(
        type = "creation",
        date = Timestamp.getUnixTimestampHex(),
        description = "Created",
        authorUsername = user!!.username
      )
    } else {
      WisdomHistoryEntry(
        type = "edit",
        date = Timestamp.getUnixTimestampHex(),
        description = "Edited",
        authorUsername = user!!.username
      )
    }
    lesson.history.add(Json.encodeToString(historyEntry))
    saveEntry(lesson)
    appCall.respond(lesson.gUID)
  }

  suspend fun httpCreateComment(
    appCall: ApplicationCall, config: WisdomCommentCreation, wisdomGUID: String
  ) {
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
    var wisdomRef: Wisdom? = null
    getEntriesFromIndexSearch(
      searchText = "^${config.wisdomGUID}$", ixNr = 1, showAll = true
    ) {
      it as Wisdom
      wisdomRef = it
    }
    if (wisdomRef == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    var comment = Wisdom()
    if (wisdomGUID.isNotEmpty()) {
      // Get existing wisdom if one got referenced
      getEntriesFromIndexSearch(
        searchText = "^$wisdomGUID$", ixNr = 1, showAll = true
      ) {
        it as Wisdom
        comment = it
      }
    }
    // Meta
    comment.type = "comment"
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
    appCall.respond(comment.gUID)
  }

  suspend fun httpWisdomQuery(appCall: ApplicationCall, config: WisdomSearchQuery, knowledgeGUID: String?) {
    // val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
    var knowledgeRef: Knowledge? = null
    KnowledgeController().getEntriesFromIndexSearch(
      searchText = "^$knowledgeGUID$", ixNr = 1, showAll = true
    ) {
      it as Knowledge
      knowledgeRef = it
    }
    if (knowledgeRef == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    // Matches Total
    var matchesAll: Sequence<MatchResult>
    // Etc
    var rating: Int
    var accuracy: Int
    val first: ArrayList<WisdomSearchResponseEntry> = arrayListOf()
    val second: ArrayList<WisdomSearchResponseEntry> = arrayListOf()
    val third: ArrayList<WisdomSearchResponseEntry> = arrayListOf()
    val elapsedMillis = measureTimeMillis {
      // Split words on each whitespace after removing duplicate whitespaces
      val cleanQuery = config.query.replace("\\s+".toRegex()) { it.value[0].toString() }
      val queryWords = cleanQuery.split("\\s".toRegex())
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
        searchText = indexQuery, ixNr = indexNumber, showAll = true
      ) {
        it as Wisdom
        rating = 0
        accuracy = 0
        // Title
        if (config.filterOverride.isEmpty() || config.filterOverride.contains("title")) {
          matchesAll = regexPattern.findAll(it.title)
          regexMatchCounts = matchesAll.count()
          if (regexMatchCounts > 0) {
            rating += 2
            accuracy += regexMatchCounts
          }
        }
        // Keywords
        if (config.filterOverride.isEmpty() || config.filterOverride.contains("keywords")) {
          matchesAll = regexPattern.findAll(it.keywords)
          regexMatchCounts = matchesAll.count()
          if (regexMatchCounts > 0) {
            rating += 2
            accuracy += regexMatchCounts
          }
        }
        // Description
        if (config.filterOverride.isEmpty() || config.filterOverride.contains("description")) {
          matchesAll = regexPattern.findAll(it.description)
          regexMatchCounts = matchesAll.count()
          if (regexMatchCounts > 0) {
            rating += 1
            accuracy += regexMatchCounts
          }
        }
        // Author
        if (config.filterOverride.isEmpty() || config.filterOverride.contains("author")) {
          matchesAll = regexPattern.findAll(it.authorUsername)
          regexMatchCounts = matchesAll.count()
          if (regexMatchCounts > 0) {
            rating += 1
            accuracy += regexMatchCounts
          }
        }
        // Discard if irrelevant
        if (rating > 0) {
          if (it.description.isNotEmpty()) {
            var description = it.description
            var truncated = false
            // Cut the description to a maximum of 200 characters if there is one
            if (it.description.length > 200) {
              description = description.substring(0..200)
              truncated = true
            }
            // Remove line breaks as this could lead to broken design
            description = description.replace("\n", " ")
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
          // Evaluate
          if (rating >= 4) {
            first.add(
              WisdomSearchResponseEntry(it, accuracy)
            )
          } else if (rating >= 3) {
            second.add(
              WisdomSearchResponseEntry(it, accuracy)
            )
          } else {
            third.add(
              WisdomSearchResponseEntry(it, accuracy)
            )
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
    response.first = first.sortedWith(compareBy { it.accuracy }).reversed()
    response.second = second.sortedWith(compareBy { it.accuracy }).reversed()
    response.third = third.sortedWith(compareBy { it.accuracy }).reversed()
    val jsonPayload = Json.encodeToString(response)
    appCall.respond(jsonPayload)
  }

  suspend fun httpWisdomReact(
    appCall: ApplicationCall, config: UniMessageReaction, wisdomGUID: String?
  ) {
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
    if (user == null) {
      appCall.respond(HttpStatusCode.Unauthorized)
      return
    }
    var wisdom: Wisdom? = null
    getEntriesFromIndexSearch(
      searchText = "^$wisdomGUID$", ixNr = 1, showAll = true
    ) {
      it as Wisdom
      wisdom = it
    }
    if (wisdom == null) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    var index = 0
    var reactionTypeExists = false
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
          // Reaction does exist and contains the user -> Return
          react.from.remove(user.username)
          if (react.from.size > 0) {
            wisdom!!.reactions[index] = Json.encodeToString(react)
          } else {
            wisdom!!.reactions.removeAt(index)
          }
          break
        } else {
          // Reaction does exist but does not contain the user -> Add user
          react.from.add(user.username)
          wisdom!!.reactions[index] = Json.encodeToString(react)
          break
        }
      }
      index++
    }
    if (!reactionTypeExists) {
      val reaction = UniMessageReaction(
        from = arrayListOf(user.username), type = config.type
      )
      wisdom!!.reactions.add(Json.encodeToString(reaction))
    }
    save(wisdom!!)
    appCall.respond(HttpStatusCode.OK)
  }

  suspend fun httpGetWisdomEntriesRelated(appCall: ApplicationCall, wisdomGUID: String?) {
    if (wisdomGUID == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    var wisdomRef: Wisdom? = null
    getEntriesFromIndexSearch(
      searchText = "^$wisdomGUID$", ixNr = 1, showAll = true
    ) {
      it as Wisdom
      wisdomRef = it
    }
    if (wisdomRef == null) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    val response = WisdomReferencesResponse()
    getEntriesFromIndexSearch(
      searchText = "^${wisdomRef!!.uID}$", ixNr = 3, showAll = true
    ) {
      it as Wisdom
      when (it.type) {
        "answer" -> response.answers.add(it)
        "comment" -> response.comments.add(it)
      }
    }
    appCall.respond(response)
  }

  suspend fun httpWisdomTopContributors(appCall: ApplicationCall, knowledgeGUID: String?) {
    var knowledgeRef: Knowledge? = null
    KnowledgeController().getEntriesFromIndexSearch(
      searchText = "^$knowledgeGUID$", ixNr = 1, showAll = true
    ) {
      it as Knowledge
      knowledgeRef = it
    }
    if (knowledgeRef == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    val contributors = mutableMapOf<String, Int>()
    getEntriesFromIndexSearch(
      searchText = "^${knowledgeRef!!.uID}$", ixNr = 2, showAll = true
    ) {
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
          username = contributor.key, imageURL = "", lessons = contributor.value
        )
      )
    }
    appCall.respond(response)
  }

  suspend fun httpDeleteWisdom(appCall: ApplicationCall, wisdomGUID: String?) {
    var wisdom: Wisdom? = null
    getEntriesFromIndexSearch(
      searchText = "^$wisdomGUID$", ixNr = 1, showAll = true
    ) {
      it as Wisdom
      wisdom = it
    }
    if (wisdom == null) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
    if (user == null || wisdom!!.authorUsername != user.username) {
      appCall.respond(HttpStatusCode.Unauthorized)
      return
    }
    wisdom!!.gUID = "?"
    wisdom!!.knowledgeUID = -1
    wisdom!!.title = "?"
    wisdom!!.description = "?"
    wisdom!!.srcWisdomUID = -1
    wisdom!!.refWisdomUID = -1
    wisdom!!.isTask = false
    saveEntry(wisdom!!)
    appCall.respond(HttpStatusCode.OK)
  }

  suspend fun httpGetWisdomEntry(appCall: ApplicationCall, wisdomGUID: String) {
    var wisdom: Wisdom? = null
    getEntriesFromIndexSearch(
      searchText = "^$wisdomGUID$", ixNr = 1, showAll = true
    ) {
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
    appCall.respond(wisdom!!)
  }

  suspend fun httpGetTasks(appCall: ApplicationCall, knowledgeGUID: String?, stateFilter: String) {
    if (knowledgeGUID == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    var knowledgeRef: Knowledge? = null
    KnowledgeController().getEntriesFromIndexSearch(
      searchText = "^$knowledgeGUID$", ixNr = 1, showAll = true
    ) {
      it as Knowledge
      knowledgeRef = it
    }
    if (knowledgeRef == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    // First, fetch all boxes
    val taskBoxesResponse = TaskBoxesResponse()
    getEntriesFromIndexSearch(
      searchText = "^${knowledgeRef!!.uID};BOX$", ixNr = 6, showAll = true
    ) {
      it as Wisdom
      if (it.finished) {
        it.finishedDate = Timestamp.getUTCTimestampFromHex(it.finishedDate)
      }
      it.dateCreated = Timestamp.getUTCTimestampFromHex(it.dateCreated)
      taskBoxesResponse.boxes.add(TaskBoxPayload(it))
      runBlocking {
      }
    }
    if (taskBoxesResponse.boxes.isEmpty()) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    // Now, get the tasks of all gathered boxes
    var isFinishedDesire = false
    if (stateFilter == "finished") isFinishedDesire = true
    for (i in 0 until taskBoxesResponse.boxes.size) {
      getEntriesFromIndexSearch(
        searchText = "^${taskBoxesResponse.boxes[i].box.uID}$", ixNr = 3, showAll = true
      ) {
        it as Wisdom
        if (it.finished == isFinishedDesire) {
          // Convert dates before sending the entries back
          if (it.finished) {
            it.finishedDate = Timestamp.getUTCTimestampFromHex(it.finishedDate)
          }
          it.dateCreated = Timestamp.getUTCTimestampFromHex(it.dateCreated)
          taskBoxesResponse.boxes[i].tasks.add(it)
        }
      }
    }
    // Respond
    appCall.respond(taskBoxesResponse)
  }

  suspend fun httpFinishWisdom(appCall: ApplicationCall, wisdomGUID: String?) {
    var wisdom: Wisdom? = null
    getEntriesFromIndexSearch(
      searchText = "^$wisdomGUID$", ixNr = 1, showAll = true
    ) {
      it as Wisdom
      wisdom = it
    }
    if (wisdom == null) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
    // If the user is unauthorized or neither the creator nor a collaborator, exit
    if (user == null ||
      (wisdom!!.authorUsername != user.username && !wisdom!!.collaborators.contains(user.username))) {
      appCall.respond(HttpStatusCode.Unauthorized)
      return
    }
    wisdom!!.hasDueDate = false
    wisdom!!.finished = true
    wisdom!!.finishedDate = Timestamp.getUnixTimestampHex()
    saveEntry(wisdom!!)
    appCall.respond(HttpStatusCode.OK)
  }

  suspend fun httpModifyWisdomContributor(
    appCall: ApplicationCall,
    wisdomGUID: String?,
    config: WisdomCollaboratorPayload
  ) {
    var wisdom: Wisdom? = null
    getEntriesFromIndexSearch(
      searchText = "^$wisdomGUID$", ixNr = 1, showAll = true
    ) {
      it as Wisdom
      wisdom = it
    }
    if (wisdom == null) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    // Add collaborator if he doesn't exist yet
    if (!wisdom!!.collaborators.contains(config.username)) {
      if (config.add) {
        wisdom!!.collaborators.add(config.username)
      }
    } else if (!config.add) {
      // Remove him if he does
      wisdom!!.collaborators.remove(config.username)
    }
  }
}
