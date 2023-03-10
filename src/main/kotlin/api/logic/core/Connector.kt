package api.logic.core

import api.misc.json.ConnectorFrame
import api.misc.json.ConnectorIncomingCall
import api.misc.json.UserWithOnlineState
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.server.plugins.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m2.Contact
import modules.m5.UniMember
import modules.m5.logic.UniChatroomController
import modules.mx.contactIndexManager
import modules.mx.logic.Log
import modules.mx.logic.Timestamp
import modules.mx.logic.Timestamp.Timestamp.now
import modules.mx.logic.UserCLIManager
import modules.mx.logic.indexFormat
import java.util.*

class Connector {
  @ExperimentalCoroutinesApi
  @DelicateCoroutinesApi
  @InternalAPI
  @ExperimentalSerializationApi
  companion object Connector : IModule {
    override val moduleNameLong = "Connector"
    override val module = "MX"
    override fun getIndexManager(): IIndexManager? {
      return null
    }

    private val connectedUsersMutex = Mutex()

    private val connectedUsers: MutableMap<String, Connection> = Collections.synchronizedMap(LinkedHashMap())

    class Connection(
      val session: DefaultWebSocketSession,
      val username: String
    ) {
      companion object {
        // var lastId = AtomicInteger(0)
      }
      // val id = "u${lastId.getAndIncrement()}"
    }

    private suspend fun addUser(
      session: DefaultWebSocketSession,
      username: String
    ): Connection? {
      if (username.isEmpty()) return null
      if (connectedUsers.size == Int.MAX_VALUE) return null // Optimistic!
      val user = Connection(session, username)
      connectedUsersMutex.withLock {
        connectedUsers[username] = user
      }
      return user
    }

    private suspend fun removeUser(
      username: String
    ) {
      if (username.isEmpty()) return
      if (!connectedUsers.containsKey(username)) return
      connectedUsersMutex.withLock {
        connectedUsers.remove(username)
      }
    }

    /**
     * The entry point for wikiric's backend Connector.
     *
     * This Connector is responsible for sending notifications to all connected users.
     */
    suspend fun DefaultWebSocketServerSession.connect() {
      val email: String
      var username = ""
      // Wait for bearer token then authorize with provided JWT Token
      try {
        for (frame in incoming) {
          when (frame) {
            is Frame.Text -> {
              // Retrieve username from validated token
              email = ServerController.buildJWTVerifier(
                      ServerController.iniVal).verify(frame.readText()).getClaim("username").asString()
              // Further, check user rights
              if (UserCLIManager.checkModuleRight(email = email, module = "M*", true)) {
                username = UserCLIManager.getUserFromEmail(email)!!.username
                break
              } else {
                this.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Unauthorized"))
                return
              }
            }

            else -> {}
          }
        }
      } catch (e: ClosedReceiveChannelException) {
        println("onClose ${closeReason.await()}")
      } catch (e: Throwable) {
        println("onError ${closeReason.await()}")
        e.printStackTrace()
      }
      if (username.isEmpty()) {
        this.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Unauthorized"))
        return
      }
      // Add this new WebSocket connection or exit if operation failed
      val currentConnection = addUser(this, username) ?: return
      currentConnection.sendFrame(
              ConnectorFrame(
                      "info", "Successfully connected to wikiric's Connector.", now()))
      // *****************************************
      // Wait for new messages and distribute them
      // *****************************************
      for (frame in incoming) {
        when (frame) {
          is Frame.Text -> {
            launch {
              currentConnection.handleFrame(frame.readText())
            }
          }

          is Frame.Close -> {
            log(Log.Type.COM, "WS CLOSE FRAME RECEIVED ${call.request.origin.remoteHost}")
            removeUser(username)
            return
          }

          else -> {}
        }
      }
    }

    suspend fun sendFrame(
      username: String,
      frame: ConnectorFrame
    ) {
      if (connectedUsers[username] == null) return
      if (!connectedUsers[username]!!.session.outgoing.isClosedForSend) {
        connectedUsers[username]!!.session.send(Json.encodeToString(frame))
      } else removeUser(username)
    }

    private suspend fun Connection.sendFrame(
      msg: ConnectorFrame
    ) {
      if (!this.session.outgoing.isClosedForSend) {
        this.session.send(Json.encodeToString(msg))
      }
    }

    private suspend fun Connection.handleFrame(
      frameText: String
    ) {
      if (frameText.isEmpty()) return
      if (frameText.startsWith("[c:CALL]")) {
        this.handleCall(frameText)
      }
    }

    private suspend fun Connection.handleCall(
      frameText: String
    ) {
      val delimiter = "[c:CALL]"
      // Check frame text
      val config: ConnectorIncomingCall
      try {
        config = Json.decodeFromString(frameText.substringAfter(delimiter))
      } catch (_: Exception) {
        return
      }
      val usernameToCall = config.usernameToCall
      if (usernameToCall.isEmpty()) return
      // Retrieve user
      val userToCall = UserCLIManager.getUserFromUsername(usernameToCall) ?: return
      val chatroomGUID: String
      if (config.chatroomGUID.isEmpty()) {
        // Check if there's a direct message chatroom available
        val directChatrooms = UniChatroomController().directChatrooms(
                this.username, userToCall.username, true)
        if (directChatrooms.chatrooms.isEmpty()) return
        chatroomGUID = directChatrooms.chatrooms.first().chatroomGUID
      } else {
        chatroomGUID = config.chatroomGUID
      }
      sendFrame(
              username = userToCall.username, frame = ConnectorFrame(
              type = "incoming call", msg = "Incoming call from ${this.username}!", date = now(),
              srcUsername = this.username, chatroomGUID = chatroomGUID))
    }

    suspend fun setOnlineState(user: Contact) {
      var notifyUsers = false
      try {
        val previousActivity = getOnlineState(userUID = user.uID)
        if (!previousActivity.online) {
          notifyUsers = true
        }
        // Set online state
        contactIndexManager!!.setIndexValue(3, user.uID, Timestamp.getUnixTimestamp().toString())
        if (notifyUsers) {
          val onlineState = getOnlineState(userUID = user.uID)
          val directChatrooms = UniChatroomController().directChatrooms(".*", user.username, false)
          if (directChatrooms.chatrooms.isNotEmpty()) {
            directChatrooms.chatrooms.forEach { chatroom ->
              chatroom.members.forEach {
                sendFrame(
                        username = Json.decodeFromString<UniMember>(it).username, frame = ConnectorFrame(
                        type = "online", msg = "${user.username} is online!", date = now(),
                        obj = Json.encodeToString(onlineState), srcUsername = user.username))
              }
            }
          }
        }
      } catch (e: Exception) {
        println(e.message)
      }
    }

    fun getOnlineState(
      username: String = "",
      userUID: Long = -1L
    ): UserWithOnlineState {
      val onlineState: UserWithOnlineState
      // First check, if there is a user connection to the Connector
      val userConnection = connectedUsers[username]
      if (userConnection != null && !userConnection.session.outgoing.isClosedForSend) {
        onlineState = UserWithOnlineState(username, online = true, recent = true, now())
        return onlineState
      }
      // User is not connected right now, so check the last activity!
      var userUIDTmp: Long = -1L
      if (userUID == -1L) {
        val ixResults = contactIndexManager!!.filterStringValues(2, indexFormat("^${username}$"))
        if (ixResults.isNotEmpty()) {
          userUIDTmp = ixResults.first()
        }
      } else {
        userUIDTmp = userUID
      }
      if (userUIDTmp != -1L) {
        val lastActivity = contactIndexManager!!.getIndexValue(3, userUIDTmp)
        if (lastActivity.isNotEmpty()) {
          val lastActivityLong = lastActivity.toLong()
          // User is considered online if last activity was no longer than 3 minutes ago
          onlineState = if (Timestamp.getUnixTimestamp().minus(lastActivityLong) < 180) {
            UserWithOnlineState(username, online = true, recent = true, Timestamp.getUTCTimestamp(lastActivityLong))
          } else {
            // User is considered as offline but with recent activity if it was no longer than 15 minutes ago
            if (Timestamp.getUnixTimestamp().minus(lastActivityLong) < 900) {
              UserWithOnlineState(username, online = false, recent = true, Timestamp.getUTCTimestamp(lastActivityLong))
            } else {
              UserWithOnlineState(username, online = false, recent = false, Timestamp.getUTCTimestamp(lastActivityLong))
            }
          }
        } else {
          onlineState = UserWithOnlineState(username, online = false, recent = false, "")
        }
      } else {
        onlineState = UserWithOnlineState(username, online = false, recent = false, "")
      }
      return onlineState
    }
  }
}
