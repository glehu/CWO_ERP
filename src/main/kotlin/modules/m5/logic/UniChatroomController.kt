package modules.m5.logic

import api.logic.core.ServerController
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.WebpushConfig
import com.google.firebase.messaging.WebpushFcmOptions
import com.google.firebase.messaging.WebpushNotification
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m5.UniChatroom
import modules.m5.UniMember
import modules.m5.UniRole
import modules.m5messages.UniMessage
import modules.m5messages.logic.UniMessagesController
import modules.mx.logic.Log
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
    val uniMessagesController = UniMessagesController()
    val clarifierSessions: MutableSet<ClarifierSession> = Collections.synchronizedSet(LinkedHashSet())
    val mutex = Mutex()
  }

  fun createChatroom(title: String): UniChatroom {
    log(Log.Type.COM, "Chatroom created")
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

  suspend fun saveChatroom(uniChatroom: UniChatroom): Int {
    return save(uniChatroom)
  }

  private fun createMember(username: String, role: String): UniMember {
    return UniMember(username, arrayListOf(role))
  }

  fun UniChatroom.addOrUpdateMember(
    username: String,
    role: UniRole? = null,
    fcmToken: String? = null,
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
      // Update
      val member: UniMember = Json.decodeFromString(this.members[indexAt])
      if (role != null) member.addRole(role)
      if (!fcmToken.isNullOrEmpty()) member.subscribeFCM(fcmToken)
      // Save
      this.members[indexAt] = Json.encodeToString(member)
    } else {
      // Create
      val member = createMember(username, Json.encodeToString(UniRole("Member")))
      if (role != null) member.addRole(role)
      if (!fcmToken.isNullOrEmpty()) member.subscribeFCM(fcmToken)
      // Save
      this.members.add(Json.encodeToString(member))
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

  fun UniChatroom.removeMember(username: String): Boolean {
    if (username.isEmpty()) return false
    var indexAt = -1
    for (uniMember in this.members) {
      indexAt++
      if (Json.decodeFromString<UniMember>(uniMember).username == username) {
        break
      }
    }
    return if (indexAt != -1) {
      this.members.removeAt(indexAt)
      true
    } else false
  }

  /**
   * Bans a member by putting him in the ban list
   */
  fun UniChatroom.banMember(
    username: String,
  ): Boolean {
    if (username.isEmpty()) return false
    // Does the member already exist in the Clarifier's ban list'?
    var found = false
    for (uniMember in this.banlist) {
      if (Json.decodeFromString<UniMember>(uniMember).username == username) {
        found = true
        break
      }
    }
    return if (!found) {
      this.banlist.add(Json.encodeToString(createMember(username, Json.encodeToString(UniRole("Banned")))))
      true
    } else false
  }

  suspend fun UniChatroom.addMessage(member: String, message: String): Boolean {
    if (!checkMessage(message)) {
      log(Log.Type.ERROR, "Message invalid (checkMessage)")
      return false
    }
    if (!this.checkIsMember(member) or this.checkIsMemberBanned(member)) {
      log(Log.Type.ERROR, "Message invalid (checkMember)")
      return false
    }
    val uniMessage = UniMessage(
      uniChatroomUID = this.uID,
      from = member,
      message = message
    )
    uniMessagesController.save(uniMessage)
    return true
  }

  private fun checkMessage(message: String): Boolean {
    return message.isNotEmpty()
  }

  private fun UniChatroom.checkIsMember(member: String): Boolean {
    if (member == "_server") return true
    var isMember = false
    for (uniMember in this.members) {
      if (Json.decodeFromString<UniMember>(uniMember).username == member) isMember = true
    }
    return isMember
  }

  private fun UniChatroom.checkIsMemberBanned(member: String): Boolean {
    if (member == "_server") return false
    var isBanned = false
    for (uniMember in this.banlist) {
      if (Json.decodeFromString<UniMember>(uniMember).username == member) isBanned = true
    }
    return isBanned
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

    fun cleanup() {
      if (connectionsToDelete.size > 0) {
        connectionsToDelete.forEach {
          connections.remove(it)
        }
        connectionsToDelete.clear()
      }
    }
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
      // Create a new Clarifier Session
      clarifierSession = ClarifierSession(uniChatroomGUID)
      clarifierSessions.add(clarifierSession!!)
    }
    // Add this new WebSocket connection
    val thisConnection = Connection(this, username)
    clarifierSession!!.connections += thisConnection
    // Retrieve Clarifier Session
    val uniChatroom = getOrCreateUniChatroom(uniChatroomGUID, username)
    if (!uniChatroom.checkIsMember(thisConnection.username)) {
      // Notify members of new registration
      val msg = Json.encodeToString(
        UniMessage(
          uniChatroomUID = uniChatroom.uID,
          from = "_server",
          message = "[s:RegistrationNotification]$username has joined ${uniChatroom.title}!"
        )
      )
      clarifierSession!!.connections.forEach {
        if (!it.session.outgoing.isClosedForSend) {
          it.session.send(msg)
        }
      }
      // Register new member
      uniChatroom.addOrUpdateMember(username, UniRole("Member"))
      mutex.withLock {
        uniChatroom.addMessage(
          member = "_server",
          message = "[s:RegistrationNotification]$username has joined ${uniChatroom.title}!"
        )
        saveChatroom(uniChatroom)
      }
    }
    // *****************************************
    // Wait for new messages and distribute them
    // *****************************************
    var uniMessage: UniMessage
    for (frame in incoming) {
      when (frame) {
        is Frame.Text -> {
          // Add new message to the chatroom
          launch {
            uniMessage = UniMessage(
              uniChatroomUID = uniChatroom.uID,
              from = thisConnection.username,
              message = frame.readText()
            )
            val msg = Json.encodeToString(uniMessage)
            clarifierSession!!.connections.forEach {
              if (!it.session.outgoing.isClosedForSend) {
                it.session.send(msg)
              } else {
                clarifierSession!!.connectionsToDelete.add(it)
              }
            }
            mutex.withLock {
              uniChatroom.addMessage(thisConnection.username, uniMessage.message)
              // Send notification to all members
              val fcmTokens: ArrayList<String> = arrayListOf()
              for (memberJson in uniChatroom.members) {
                val member: UniMember = Json.decodeFromString(memberJson)
                if (member.firebaseCloudMessagingToken.isEmpty()) continue
                fcmTokens.add(member.firebaseCloudMessagingToken)
              }
              if (fcmTokens.isNotEmpty()) {
                /*
                 Build the notification
                 If there's a parentGUID, then this chatroom must be a subchat
                 */
                lateinit var destination: String
                lateinit var subchatGUID: String
                if (uniChatroom.parentGUID.isNotEmpty()) {
                  destination = uniChatroom.parentGUID
                  subchatGUID = uniChatroomGUID
                } else {
                  destination = uniChatroomGUID
                  subchatGUID = ""
                }
                val message = MulticastMessage.builder()
                  .setWebpushConfig(
                    WebpushConfig.builder()
                      .setNotification(
                        WebpushNotification(
                          uniChatroom.title,
                          "${thisConnection.username} has sent a message."
                        )
                      )
                      .setFcmOptions(
                        WebpushFcmOptions
                          .withLink("/apps/clarifier/wss/$destination")
                      )
                      .putData("dlType", "clarifier")
                      .putData("dlDest", "/apps/clarifier/wss/$destination")
                      .putData("subchatGUID", subchatGUID)
                      .build()
                  )
                  .addAllTokens(fcmTokens)
                  .build()
                FirebaseMessaging.getInstance().sendMulticast(message)
              }
              clarifierSession!!.cleanup()
            }
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
      // Create a new Clarifier Session and join it
      uniChatroom = createChatroom(uniChatroomGUID)
      uniChatroom.addOrUpdateMember(member, UniRole("owner"))
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
    if (this.firebaseCloudMessagingToken.isEmpty()) {
      log(Log.Type.SYS, "User ${this.username} subscribed to FCM Push Notifications")
    } else {
      if (this.firebaseCloudMessagingToken != fcmToken) {
        log(Log.Type.SYS, "User ${this.username} updated FCM Push Notifications subscription")
      } else return
    }
    this.firebaseCloudMessagingToken = fcmToken
  }

  /**
   * Removes the Firebase Cloud Messaging Token from this member
   */
  private fun UniMember.unsubscribeFCM() {
    this.firebaseCloudMessagingToken = ""
    log(Log.Type.SYS, "User ${this.username} unsubscribed from FCM Push Notifications")
  }
}
