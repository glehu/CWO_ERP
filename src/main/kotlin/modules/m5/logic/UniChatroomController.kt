package modules.m5.logic

import api.logic.core.ServerController
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m5.UniChatroom
import modules.m5.UniMember
import modules.m5.UniMessage
import modules.m5.UniRole
import modules.mx.logic.Log
import modules.mx.logic.Timestamp
import modules.mx.logic.UserCLIManager
import modules.mx.uniChatroomIndexManager
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

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
    val connections: MutableSet<Connection> = Collections.synchronizedSet(LinkedHashSet())
    val connectionsToDelete: MutableSet<Connection> = Collections.synchronizedSet(LinkedHashSet())
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

  private fun createMember(username: String, role: String): UniMember {
    return UniMember(username, arrayListOf(role))
  }

  fun UniChatroom.addMember(username: String, role: UniRole): Boolean {
    if (username.isEmpty()) return false
    val payload = Json.encodeToString(createMember(username, Json.encodeToString(role)))
    val indexAt = this.members.indexOf(username)
    if (indexAt != -1) {
      this.members[indexAt] = payload
    } else {
      this.members.add(payload)
    }
    return true
  }

  private fun UniChatroom.removeMember(username: String) {
    var indexAt = -1
    for (uniMember in this.members) {
      indexAt++
      if (Json.decodeFromString<UniMember>(uniMember).username == username) {
        break
      }
    }
    if (indexAt != -1) this.members.removeAt(indexAt)
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
    this.messages.add(Json.encodeToString(UniMessage(member, message, Timestamp.now())))
    return true
  }

  private fun UniChatroom.addMessage(member: String, uniMessage: UniMessage): Boolean {
    return this.addMessage(member, uniMessage.message)
  }

  private fun checkMessage(message: String): Boolean {
    return message.isNotEmpty()
  }

  private fun UniChatroom.checkMember(member: String): Boolean {
    var isMember = false
    for (uniMember in this.members) {
      if (Json.decodeFromString<UniMember>(uniMember).username == member) isMember = true
    }
    if (!isMember) return false

    var isBanned = false
    for (uniMember in this.banlist) {
      if (Json.decodeFromString<UniMember>(uniMember).username == member) isBanned = true
    }
    if (isBanned) return false
    return true
  }

  class Connection(val session: DefaultWebSocketSession, val username: String) {
    companion object {
      var lastId = AtomicInteger(0)
    }

    val id = "u${lastId.getAndIncrement()}"
  }

  suspend fun DefaultWebSocketServerSession.startSession(appCall: ApplicationCall) {
    val uniChatroomGUID = appCall.parameters["unichatroomGUID"]
    if (uniChatroomGUID.isNullOrEmpty()) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }

    var authSuccess = false
    var terminated = false
    var username = ""

    // Wait for bearer token then authorize with provided JWT Token
    while (!authSuccess || terminated) {
      for (frame in incoming) {
        when (frame) {
          is Frame.Text -> {
            // Retrieve username from validated token
            username = ServerController
              .buildJWTVerifier(ServerController.iniVal)
              .verify(frame.readText())
              .getClaim("username")
              .asString()
            // Further, check user rights
            if (
              UserCLIManager()
                .checkModuleRight(
                  username = username,
                  module = "M5"
                )
            ) {
              authSuccess = true
              send(
                Json.encodeToString(
                  UniMessage(
                    "_server",
                    "Logged in as $username",
                    Timestamp.now()
                  )
                )
              )
              break
            } else {
              this.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Unauthorized"))
              terminated = true
              break
            }
          }
          else -> {}
        }
      }
    }
    if (!authSuccess) return

    // Add this new WebSocket connection
    val thisConnection = Connection(this, username)
    connections += thisConnection

    // Retrieve Clarifier Session
    var uniChatroom = getOrCreateUniChatroom(uniChatroomGUID, thisConnection.username)

    // Send share link
    send(Json.encodeToString(UniMessage("_server", uniChatroom.chatroomGUID, Timestamp.now())))

    // Send all messages
    for (msg in uniChatroom.messages) {
      send(msg)
    }

    // Wait for new messages and distribute them
    for (frame in incoming) {
      when (frame) {
        is Frame.Text -> {
          val uniMessage = UniMessage(thisConnection.username, frame.readText(), Timestamp.now())
          runBlocking {
            connections.forEach {
              if (!it.session.outgoing.isClosedForSend) {
                withTimeout(Duration.ofSeconds(5)) {
                  it.session.send(Json.encodeToString(uniMessage))
                }
              } else {
                connectionsToDelete.add(it)
              }
            }

            uniChatroom = getChatroom(uniChatroom.chatroomGUID)!!
            uniChatroom.addMessage(thisConnection.username, uniMessage)
            // Remove closed connections
            if (connectionsToDelete.size > 0) {
              connectionsToDelete.forEach {
                uniChatroom.removeMember(it.username)
                connections.remove(it)
              }
              connectionsToDelete.clear()
            }
            saveChatroom(uniChatroom)
          }
        }
        is Frame.Close -> {
          connections.remove(thisConnection)
        }
        else -> {}
      }
    }
  }

  private suspend fun getOrCreateUniChatroom(
    uniChatroomGUID: String,
    member: String,
  ): UniChatroom {
    // Does the requested Clarifier Session exist?
    var uniChatroom = getChatroom(uniChatroomGUID)
    if (uniChatroom == null) {
      // Create a new Clarifier Session
      uniChatroom = createChatroom(uniChatroomGUID)
      uniChatroom.addMember(member, UniRole("owner"))
      // Join existing one
    } else {
      uniChatroom.addMember(member, UniRole("member"))
    }
    saveChatroom(uniChatroom)
    return uniChatroom
  }
}
