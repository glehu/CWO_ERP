package modules.m5.logic

import api.logic.core.Server
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m5.UniChatroom
import modules.mx.logic.Log
import modules.mx.logic.Timestamp
import modules.mx.uniChatroomIndexManager
import java.util.*

@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
class UniChatroomController : IModule {
  override val moduleNameLong = "UniChatroomController"
  override val module = "M5"
  override fun getIndexManager(): IIndexManager {
    return uniChatroomIndexManager!!
  }

  companion object {
    val connections: MutableSet<Server.Connection> = Collections.synchronizedSet(LinkedHashSet())
    val connectionsToDelete: MutableSet<Server.Connection> = Collections.synchronizedSet(LinkedHashSet())
  }

  fun createChatroom(title: String): UniChatroom {
    log(Log.LogType.COM, "Chatroom created")
    return UniChatroom(-1, title)
  }

  fun getChatroom(chatroomGUID: String): UniChatroom? {
    var uniChatroom: UniChatroom?
    synchronized(this) {
      uniChatroom = null
      getEntriesFromIndexSearch(chatroomGUID, 2, true) {
        uniChatroom = it as UniChatroom
      }
    }
    return uniChatroom
  }

  suspend fun saveChatroom(uniChatroom: UniChatroom) {
    save(uniChatroom)
  }

  fun UniChatroom.addMember(member: String, role: String): Boolean {
    this.members[member] = role
    log(Log.LogType.COM, "Member added")
    return true
  }

  fun UniChatroom.addMessage(member: String, message: String): Boolean {
    if (!checkMessage(message)) {
      log(Log.LogType.ERROR, "Message invalid (checkMessage)")
      return false
    }
    if (!this.checkMember(member)) {
      log(Log.LogType.ERROR, "Message invalid (checkMember)")
      return false
    }
    this.messages.add(Json.encodeToString(UniMessage(member, Timestamp.getUnixTimestampHex(), message)))
    log(Log.LogType.COM, "Message added")
    return true
  }

  private fun checkMessage(message: String): Boolean {
    return message.isNotEmpty()
  }

  private fun UniChatroom.checkMember(member: String): Boolean {
    if (!this.members.containsKey(member)) return false
    if (this.banlist.containsKey(member)) return false
    return true
  }

  suspend fun DefaultWebSocketServerSession.startSession(call: ApplicationCall) {
    val routePar = call.parameters["unichatroomGUID"]
    if (routePar.isNullOrEmpty()) {
      call.respond(HttpStatusCode.BadRequest)
      return
    }
    var uniChatroom = getChatroom(routePar)
    if (uniChatroom == null) uniChatroom = createChatroom(routePar)

    val thisConnection = Server.Connection(this, "")
    connections += thisConnection

    uniChatroom.addMember(thisConnection.id, "member")
    saveChatroom(uniChatroom)
    send("Logged in as [${thisConnection.id}] for Clarifier Session ${uniChatroom.title}")
    send("SessionID: ${uniChatroom.chatroomGUID}")

    send("Members:")
    for ((user, role) in uniChatroom.members) {
      send("$user ($role)")
    }

    send("Messages:")
    for (msg in uniChatroom.messages) {
      val uniMessage = Json.decodeFromString<UniMessage>(msg)
      send("[${uniMessage.from}]: ${uniMessage.message}")
    }

    for (frame in incoming) {
      when (frame) {
        is Frame.Text -> {
          val receivedText = frame.readText()
          val textWithUsername = "[${thisConnection.id}]: $receivedText"
          connections.forEach {
            if (!it.session.outgoing.isClosedForSend) {
              it.session.send(textWithUsername)
            } else {
              connectionsToDelete.add(it)
            }
          }
          uniChatroom = getChatroom(uniChatroom!!.chatroomGUID)
          uniChatroom!!.addMessage(thisConnection.id, receivedText)
          saveChatroom(uniChatroom)
          connectionsToDelete.forEach {
            connections.remove(it)
          }
        }
        is Frame.Close -> {
          connections.remove(thisConnection)
        }
        else -> {}
      }
    }
  }
}
