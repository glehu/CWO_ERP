package modules.m7wisdom.logic

import api.logic.core.ServerController
import api.misc.json.WisdomAnswerCreation
import api.misc.json.WisdomCommentCreation
import api.misc.json.WisdomLessonCreation
import api.misc.json.WisdomQuestionCreation
import api.misc.json.WisdomSearchQuery
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m7knowledge.Knowledge
import modules.m7knowledge.logic.KnowledgeController
import modules.m7wisdom.Wisdom
import modules.mx.logic.UserCLIManager
import modules.mx.wisdomIndexManager

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

  suspend fun httpCreateQuestion(appCall: ApplicationCall, config: WisdomQuestionCreation) {
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
    var knowledgeRef: Knowledge? = null
    KnowledgeController().getEntriesFromIndexSearch(
      searchText = "^${config.knowledgeGUID}$",
      ixNr = 1,
      showAll = true
    ) {
      it as Knowledge
      knowledgeRef = it
    }
    if (knowledgeRef == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    val question = Wisdom()
    // Meta
    question.type = "question"
    question.knowledgeUID = knowledgeRef!!.uID
    question.authorUsername = user!!.username
    // Info
    question.title = config.title
    question.description = config.description
    question.keywords = config.keywords
    question.categories = config.categories
    saveEntry(question)
    appCall.respond(question.gUID)
  }

  suspend fun httpCreateAnswer(appCall: ApplicationCall, config: WisdomAnswerCreation) {
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
    var wisdomRef: Wisdom? = null
    getEntriesFromIndexSearch(
      searchText = "^${config.wisdomGUID}$",
      ixNr = 3,
      showAll = true
    ) {
      it as Wisdom
      wisdomRef = it
    }
    if (wisdomRef == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    val answer = Wisdom()
    // Meta
    answer.type = "answer"
    answer.knowledgeUID = wisdomRef!!.knowledgeUID
    answer.srcWisdomUID = wisdomRef!!.uID
    answer.authorUsername = user!!.username
    // Info
    answer.title = config.title
    answer.description = config.description
    answer.keywords = config.keywords
    answer.copyContent = config.copyContent
    saveEntry(answer)
    appCall.respond(answer.gUID)
  }

  suspend fun httpCreateLesson(appCall: ApplicationCall, config: WisdomLessonCreation) {
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
    var knowledgeRef: Knowledge? = null
    KnowledgeController().getEntriesFromIndexSearch(
      searchText = "^${config.knowledgeGUID}$",
      ixNr = 1,
      showAll = true
    ) {
      it as Knowledge
      knowledgeRef = it
    }
    if (knowledgeRef == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    val answer = Wisdom()
    // Meta
    answer.type = "lesson"
    answer.knowledgeUID = knowledgeRef!!.uID
    answer.authorUsername = user!!.username
    // Info
    answer.title = config.title
    answer.description = config.description
    answer.keywords = config.keywords
    answer.copyContent = config.copyContent
    answer.categories = config.categories
    saveEntry(answer)
    appCall.respond(answer.gUID)
  }

  suspend fun httpCreateComment(appCall: ApplicationCall, config: WisdomCommentCreation) {
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
    var wisdomRef: Wisdom? = null
    getEntriesFromIndexSearch(
      searchText = "^${config.wisdomGUID}$",
      ixNr = 3,
      showAll = true
    ) {
      it as Wisdom
      wisdomRef = it
    }
    if (wisdomRef == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    val answer = Wisdom()
    // Meta
    answer.type = "comment"
    answer.knowledgeUID = wisdomRef!!.knowledgeUID
    answer.refWisdomUID = wisdomRef!!.uID
    answer.authorUsername = user!!.username
    // Info
    answer.title = config.title
    answer.description = config.description
    answer.keywords = config.keywords
    saveEntry(answer)
    appCall.respond(answer.gUID)
  }

  suspend fun httpWisdomQuery(appCall: ApplicationCall, config: WisdomSearchQuery, knowledgeGUID: String?) {
    val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))
    var knowledgeRef: Knowledge? = null
    KnowledgeController().getEntriesFromIndexSearch(
      searchText = "^$knowledgeGUID$",
      ixNr = 1,
      showAll = true
    ) {
      it as Knowledge
      knowledgeRef = it
    }
    if (knowledgeRef == null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    val responseFirst: ArrayList<Wisdom> = arrayListOf()
    val responseSecond: ArrayList<Wisdom> = arrayListOf()
    // Split words on each whitespace after removing duplicate whitespaces
    val cleanQuery = config.query.replace("\\s+".toRegex()) { it.value[0].toString() }
    val queryWords = cleanQuery.split("\\s".toRegex())
    val queryFormatted = StringBuilder()
    queryFormatted.append('(')
    for ((amount, word) in queryWords.withIndex()) {
      if (amount > 0) queryFormatted.append("|")
      queryFormatted.append(word)
    }
    queryFormatted.append(')')
    val regexPattern = queryFormatted.toString().toRegex(RegexOption.IGNORE_CASE)
    getEntriesFromIndexSearch(
      searchText = "^${knowledgeRef!!.uID}$",
      ixNr = 2,
      showAll = true
    ) {
      it as Wisdom
      if (it.title.contains(regexPattern)) {
        responseFirst.add(it)
      } else if (it.keywords.contains(regexPattern)) {
        responseFirst.add(it)
      } else if (it.description.contains(regexPattern)) {
        responseSecond.add(it)
      }
    }
    if (responseFirst.isEmpty() && responseSecond.isEmpty()) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    responseFirst.addAll(responseSecond)
    appCall.respond(Json.encodeToString(responseFirst))
  }
}
