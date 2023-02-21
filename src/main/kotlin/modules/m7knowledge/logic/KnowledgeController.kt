package modules.m7knowledge.logic

import api.logic.core.ServerController
import api.misc.json.KnowledgeCategoryEdit
import api.misc.json.KnowledgeCreation
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
import modules.m5.logic.UniChatroomController
import modules.m7knowledge.Knowledge
import modules.m7wisdom.WisdomCategory
import modules.mx.knowledgeIndexManager
import modules.mx.logic.UserCLIManager

@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
class KnowledgeController : IModule {
  override val moduleNameLong = "KnowledgeController"
  override val module = "M7"
  override fun getIndexManager(): IIndexManager {
    return knowledgeIndexManager!!
  }

  companion object {
    val mutex = Mutex()
  }

  private suspend fun saveEntry(knowledge: Knowledge): Long {
    var uID: Long
    mutex.withLock {
      uID = save(knowledge)
    }
    return uID
  }

  suspend fun httpGetKnowledgeFromUniChatroomGUID(appCall: ApplicationCall, mainChatroomGUID: String) {
    var knowledge: Knowledge? = null
    KnowledgeController().getEntriesFromIndexSearch(
            searchText = "^$mainChatroomGUID$", ixNr = 2, showAll = true
    ) {
      it as Knowledge
      knowledge = it
    }
    if (knowledge == null) {
      appCall.respond(HttpStatusCode.NotFound)
    } else {
      appCall.respond(knowledge!!)
    }
  }

  suspend fun httpGetKnowledgeFromGUID(appCall: ApplicationCall, knowledgeGUID: String) {
    var knowledge: Knowledge? = null
    KnowledgeController().getEntriesFromIndexSearch(
            searchText = "^$knowledgeGUID$", ixNr = 1, showAll = true
    ) {
      it as Knowledge
      knowledge = it
    }
    if (knowledge == null) {
      appCall.respond(HttpStatusCode.NotFound)
    } else {
      appCall.respond(knowledge!!)
    }
  }

  suspend fun httpCreateKnowledge(appCall: ApplicationCall, config: KnowledgeCreation) {
    var knowledge: Knowledge? = null
    KnowledgeController().getEntriesFromIndexSearch(
            searchText = "^${config.mainChatroomGUID}$", ixNr = 2, showAll = true
    ) {
      it as Knowledge
      knowledge = it
    }
    if (knowledge != null) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    knowledge = Knowledge()
    knowledge!!.mainChatroomGUID = config.mainChatroomGUID
    knowledge!!.title = config.title
    knowledge!!.description = config.description
    knowledge!!.keywords = config.keywords
    knowledge!!.isPrivate = config.isPrivate
    KnowledgeController().saveEntry(knowledge!!)
    appCall.respond(knowledge!!.guid)
  }

  suspend fun httpEditKnowledgeCategories(
    appCall: ApplicationCall, config: KnowledgeCategoryEdit, knowledgeGUID: String?
  ) {
    var knowledge: Knowledge? = null
    getEntriesFromIndexSearch(
            searchText = "^$knowledgeGUID$", ixNr = 1, showAll = true
    ) {
      it as Knowledge
      knowledge = it
    }
    if (knowledge == null) {
      appCall.respond(HttpStatusCode.NotFound)
      return
    }
    when (config.action.lowercase()) {
      "add" -> {
        if (knowledge!!.categories.isNotEmpty()) {
          // Check if it contains the category already
          var category: WisdomCategory
          for (categoryJson in knowledge!!.categories) {
            category = Json.decodeFromString(categoryJson)
            if (category.category.lowercase() == config.category.lowercase()) {
              appCall.respond(HttpStatusCode.NotAcceptable)
              return
            }
          }
        }
        val json = Json.encodeToString(WisdomCategory(config.category))
        knowledge!!.categories.add(json)
        saveEntry(knowledge!!)
        appCall.respond(HttpStatusCode.OK)
        return
      }

      "remove" -> {
        // Check if it contains the category
        var category: WisdomCategory
        for ((index, categoryJson) in knowledge!!.categories.withIndex()) {
          category = Json.decodeFromString(categoryJson)
          if (category.category.lowercase() == config.category.lowercase()) {
            knowledge!!.categories.removeAt(index)
            saveEntry(knowledge!!)
            appCall.respond(HttpStatusCode.OK)
            return
          }
        }
        appCall.respond(HttpStatusCode.NotFound)
        return
      }

      else -> {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
    }
  }

  /**
   * Check if user is authorized in case the knowledge is private.
   * @return [Boolean] "true" if the user is authorized and "false" if not.
   */
  suspend fun httpCanAccessKnowledge(appCall: ApplicationCall, knowledgeRef: Knowledge): Boolean {
    if (knowledgeRef.isPrivate) {
      if (knowledgeRef.mainChatroomGUID.isNotEmpty()) {
        with(UniChatroomController()) {
          val chatroom = getChatroom(knowledgeRef.mainChatroomGUID)
          if (chatroom == null) {
            appCall.respond(HttpStatusCode.Conflict)
            return false
          }
          if (!chatroom.checkIsMember(
                    UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))!!.username
            )) {
            appCall.respond(HttpStatusCode.Forbidden)
            return false
          } else return true
        }
      }
    }
    return true
  }
}
