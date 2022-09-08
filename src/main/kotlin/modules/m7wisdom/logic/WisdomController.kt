package modules.m7wisdom.logic

import api.logic.core.ServerController
import api.misc.json.UniMessageReaction
import api.misc.json.WisdomAnswerCreation
import api.misc.json.WisdomCommentCreation
import api.misc.json.WisdomLessonCreation
import api.misc.json.WisdomQuestionCreation
import api.misc.json.WisdomReferencesResponse
import api.misc.json.WisdomSearchQuery
import api.misc.json.WisdomSearchResponse
import api.misc.json.WisdomSearchResponseEntry
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
import modules.m7knowledge.Knowledge
import modules.m7knowledge.logic.KnowledgeController
import modules.m7wisdom.Wisdom
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
    if (wisdomGUID.isNotEmpty()) {
      // Get existing wisdom if one got referenced
      getEntriesFromIndexSearch(
        searchText = "^$wisdomGUID$", ixNr = 1, showAll = true
      ) {
        it as Wisdom
        lesson = it
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
    saveEntry(lesson)
    appCall.respond(lesson.gUID)
  }

  suspend fun httpCreateComment(
    appCall: ApplicationCall, config: WisdomCommentCreation, wisdomGUID: String
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
    var matchesTitleAll: Sequence<MatchResult>
    var matchesKeywordsAll: Sequence<MatchResult>
    var matchesDescriptionAll: Sequence<MatchResult>
    // Atomic Matches
    val matchesTitle: MutableList<String> = mutableListOf()
    val matchesKeywords: MutableList<String> = mutableListOf()
    val matchesDescription: MutableList<String> = mutableListOf()
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
      getEntriesFromIndexSearch(
        searchText = "^${knowledgeRef!!.uID}$", ixNr = 2, showAll = true
      ) {
        it as Wisdom
        rating = 0
        accuracy = 0
        matchesTitle.clear()
        matchesKeywords.clear()
        matchesDescription.clear()
        // Title
        matchesTitleAll = regexPattern.findAll(it.title)
        if (matchesTitleAll.count() > 0) {
          rating += 2
          for (match in matchesTitleAll) {
            if (!matchesTitle.contains(match.value)) {
              matchesTitle.add(match.value)
            }
          }
          accuracy += matchesTitle.count()
        }
        // Keywords
        matchesKeywordsAll = regexPattern.findAll(it.keywords)
        if (matchesKeywordsAll.count() > 0) {
          rating += 2
          for (match in matchesKeywordsAll) {
            if (!matchesKeywords.contains(match.value)) {
              matchesKeywords.add(match.value)
            }
          }
          accuracy += matchesKeywords.count()
        }
        // Description
        matchesDescriptionAll = regexPattern.findAll(it.description)
        if (matchesDescriptionAll.count() > 0) {
          rating += 1
          for (match in matchesDescriptionAll) {
            if (!matchesDescription.contains(match.value)) {
              matchesDescription.add(match.value)
            }
          }
          accuracy += matchesDescription.count()
        }
        // Evaluate
        if (rating >= 4) {
          first.add(
            WisdomSearchResponseEntry(it, accuracy)
          )
        } else if (rating >= 3) {
          second.add(
            WisdomSearchResponseEntry(it, accuracy)
          )
        } else if (rating >= 1) {
          third.add(
            WisdomSearchResponseEntry(it, accuracy)
          )
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
    appCall.respond(Json.encodeToString(response))
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
    if (wisdomGUID == null) return
    val response = WisdomReferencesResponse()
    getEntriesFromIndexSearch(
      searchText = "^$wisdomGUID$", ixNr = 3, showAll = true
    ) {
      it as Wisdom
      when (it.type) {
        "answer" -> response.answers.add(it)
        "comment" -> response.comments.add(it)
      }
    }
    appCall.respond(response)
  }
}
