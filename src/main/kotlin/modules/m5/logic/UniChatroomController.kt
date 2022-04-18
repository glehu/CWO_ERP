package modules.m5.logic

import api.logic.core.ServerController
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    val clarifierSessions: MutableSet<ClarifierSession> = Collections.synchronizedSet(LinkedHashSet())
    val mutex = Mutex()
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

  fun UniChatroom.addOrUpdateMember(
    username: String,
    role: UniRole? = null,
    fcmToken: String? = null
  ): Boolean {
    if (username.isEmpty()) return false
    // Does the member already exist in the Clarifier Session?
    var indexAt = -1
    var found = false
    for (uniMember in this.members) {
      indexAt++
      if (Json.decodeFromString<UniMember>(uniMember).username == username) {
        found = true
        break
      }
    }
    if (indexAt != -1 && found) {
      val member: UniMember = Json.decodeFromString(this.members[indexAt])
      // Update
      if (role != null) member.addRole(role)
      if (!fcmToken.isNullOrEmpty()) member.subscribeFCM(fcmToken)
      // Save
      this.members[indexAt] = Json.encodeToString(member)
    } else {
      this.members.add(Json.encodeToString(createMember(username, Json.encodeToString(role))))
    }
    return true
  }

  /**
   * Adds a role to this member
   */
  private fun UniMember.addRole(role: UniRole) {
    // Does the member already have this role?
    var found = false
    for (rle in this.roles) {
      if (Json.decodeFromString<UniRole>(rle).name == role.name) {
        found = true
        break
      }
    }
    if (!found) this.roles.add(Json.encodeToString(role))
  }

  /**
   * Removes a role from this member
   */
  fun UniMember.removeRole(role: UniRole) {
    var indexAt = -1
    for (uniRole in this.roles) {
      indexAt++
      if (Json.decodeFromString<UniRole>(uniRole).name == role.name) {
        break
      }
    }
    if (indexAt != -1) this.roles.removeAt(indexAt)
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

  class ClarifierSession(val chatroomGUID: String) {
    val connections: MutableSet<Connection> = Collections.synchronizedSet(LinkedHashSet())
    val connectionsToDelete: MutableSet<Connection> = Collections.synchronizedSet(LinkedHashSet())
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
    // Does this Clarifier Session exist?
    var found = false
    var clarifierSession: ClarifierSession? = null
    clarifierSessions.forEach {
      if (it.chatroomGUID == uniChatroomGUID) {
        found = true
        clarifierSession = it
      }
    }
    if (!found) {
      clarifierSession = ClarifierSession(uniChatroomGUID)
      clarifierSessions.add(clarifierSession!!)
    }
    // Add this new WebSocket connection
    val thisConnection = Connection(this, username)
    clarifierSession!!.connections += thisConnection
    // Retrieve Clarifier Session
    var uniChatroom = getOrCreateUniChatroom(uniChatroomGUID, thisConnection.username)
    // Send login info and share link
    clarifierSession!!.connections.forEach {
      if (!it.session.outgoing.isClosedForSend) {
        it.session.send(
          Json.encodeToString(
            UniMessage(
              from = "_server",
              message = "[s:LoginNotification]$username has joined the Clarifier session! Say hi!",
              timestamp = Timestamp.now()
            )
          )
        )
      } else {
        clarifierSession!!.connectionsToDelete.add(it)
      }
    }
    if (clarifierSession!!.connectionsToDelete.size > 0) {
      clarifierSession!!.connectionsToDelete.forEach {
        clarifierSession!!.connections.remove(it)
      }
      clarifierSession!!.connectionsToDelete.clear()
    }
    // Wait for new messages and distribute them
    var uniMessage: UniMessage
    for (frame in incoming) {
      when (frame) {
        is Frame.Text -> {
          mutex.withLock {
            uniMessage = UniMessage(thisConnection.username, frame.readText(), Timestamp.now())
            clarifierSession!!.connections.forEach {
              if (!it.session.outgoing.isClosedForSend) {
                it.session.send(Json.encodeToString(uniMessage))
              } else {
                clarifierSession!!.connectionsToDelete.add(it)
              }
            }
            // Add new message to the chatroom
            uniChatroom = getChatroom(uniChatroom.chatroomGUID)!!
            uniChatroom.addMessage(thisConnection.username, uniMessage)
            saveChatroom(uniChatroom)
          }
          // Send notification to all members
          val fcmTokens: ArrayList<String> = arrayListOf()
          for (memberJson in uniChatroom.members) {
            val member: UniMember = Json.decodeFromString(memberJson)
            if (member.firebaseCloudMessagingToken.isEmpty()) continue
            fcmTokens.add(member.firebaseCloudMessagingToken)
          }
          val message = MulticastMessage.builder()
            .setNotification(
              Notification.builder()
                .setTitle(uniChatroom.title)
                .setBody("${thisConnection.username} has sent a message.")
                .build()
            )
            .addAllTokens(fcmTokens)
            .build()
          FirebaseMessaging.getInstance().sendMulticast(message)
          // Remove closed connections
          if (clarifierSession!!.connectionsToDelete.size > 0) {
            clarifierSession!!.connectionsToDelete.forEach {
              clarifierSession!!.connections.remove(it)
            }
            clarifierSession!!.connectionsToDelete.clear()
          }
        }
        is Frame.Close -> {
          clarifierSession!!.connections.remove(thisConnection)
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
      uniChatroom.addOrUpdateMember(member, UniRole("owner"))
      // Join existing one
    } else {
      uniChatroom.addOrUpdateMember(member, UniRole("member"))
    }
    saveChatroom(uniChatroom)
    return uniChatroom
  }

  /**
   * Adds a Firebase Cloud Messaging Token to this member
   */
  private fun UniMember.subscribeFCM(fcmToken: String) {
    if (fcmToken.isEmpty()) {
      this.unsubscribeFCM()
      return
    }
    this.firebaseCloudMessagingToken = fcmToken
  }

  /**
   * Removes the Firebase Cloud Messaging Token from this member
   */
  private fun UniMember.unsubscribeFCM() {
    this.firebaseCloudMessagingToken = ""
  }
}
