package modules.m5.logic

import api.logic.core.ServerController
import api.misc.json.UniChatroomEditMessage
import api.misc.json.UniChatroomReactMessage
import api.misc.json.UniChatroomReactMessageResponse
import api.misc.json.UniMessageReaction
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.WebpushConfig
import com.google.firebase.messaging.WebpushFcmOptions
import com.google.firebase.messaging.WebpushNotification
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
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
import modules.m5.UniChatroom
import modules.m5.UniMember
import modules.m5.UniRole
import modules.m5messages.UniMessage
import modules.m5messages.logic.UniMessagesController
import modules.mx.logic.Log
import modules.mx.logic.Timestamp
import modules.mx.logic.UserCLIManager
import modules.mx.logic.roundTo
import modules.mx.uniChatroomIndexManager
import modules.mx.uniMessagesIndexManager
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
    val uniMessagesController = UniMessagesController()
    val clarifierSessions: MutableSet<ClarifierSession> = Collections.synchronizedSet(LinkedHashSet())
    val mutexChatroom = Mutex()
    val mutexMessages = Mutex()
  }

  suspend fun createChatroom(title: String, type: String): UniChatroom {
    log(Log.Type.COM, "Chatroom created")
    val chatroom = UniChatroom(-1, title)
    chatroom.type = type
    return chatroom
  }

  suspend fun getChatroom(chatroomGUID: String): UniChatroom? {
    var uniChatroom: UniChatroom?
    mutexChatroom.withLock {
      uniChatroom = null
      getEntriesFromIndexSearch(chatroomGUID, 2, true) {
        uniChatroom = it as UniChatroom
      }
    }
    return uniChatroom
  }

  /**
   * @return the main [UniChatroom] of the provided subchat or the subchat if it is the main chatroom.
   */
  suspend fun getMainChatroom(chatroomGUID: String): UniChatroom? {
    val chatroom = getChatroom(chatroomGUID) ?: return null
    return getMainChatroom(chatroom)
  }

  private suspend fun getMainChatroom(chatroom: UniChatroom): UniChatroom? {
    // No Subchats allowed
    val mainChatroom = if (chatroom.parentGUID.isNotEmpty()) {
      getChatroom(chatroom.parentGUID)
    } else chatroom
    return mainChatroom
  }

  suspend fun saveChatroom(uniChatroom: UniChatroom): Int {
    var uID: Int
    mutexChatroom.withLock {
      uID = save(uniChatroom)
    }
    return uID
  }

  private fun createMember(username: String, role: String): UniMember {
    return UniMember(username, arrayListOf(role))
  }

  suspend fun UniChatroom.addOrUpdateMember(
    username: String,
    role: UniRole? = null,
    fcmToken: String? = null,
    pubKeyPEM: String? = null,
    imageSnippetURL: String? = null
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
      if (!pubKeyPEM.isNullOrEmpty()) member.addRSAPubKeyPEM(pubKeyPEM)
      if (!imageSnippetURL.isNullOrEmpty()) member.imageURL = imageSnippetURL
      // Save
      this.members[indexAt] = Json.encodeToString(member)
    } else {
      // Create
      val member = createMember(username, Json.encodeToString(UniRole("Member")))
      if (role != null) member.addRole(role)
      if (!fcmToken.isNullOrEmpty()) member.subscribeFCM(fcmToken)
      if (!pubKeyPEM.isNullOrEmpty()) member.addRSAPubKeyPEM(pubKeyPEM)
      if (!imageSnippetURL.isNullOrEmpty()) member.imageURL = imageSnippetURL
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
    // Does the member already exist in the Clarifier's ban list?
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

  suspend fun UniChatroom.addMessage(member: String, message: String, gUID: String = ""): Boolean {
    if (!checkMessage(message)) {
      log(Log.Type.ERROR, "Message invalid (checkMessage)")
      return false
    }
    if (!this.checkIsMember(member) or this.checkIsMemberBanned(member)) {
      log(Log.Type.ERROR, "Message invalid (checkMember)")
      return false
    }
    val uniMessage = UniMessage(
            uniChatroomUID = this.uID, from = member, message = message
    )
    if (gUID.isNotEmpty()) uniMessage.gUID = gUID
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

  fun UniChatroom.checkIsMemberBanned(username: String, isEmail: Boolean = false): Boolean {
    // Exit if there are no banned members (or if server is being checked)
    if (this.banlist.size < 1 || username == "_server") {
      return false
    }
    var usernameTmp: String
    // If an email was provided, convert it to a username first
    val usernameToCheck = if (isEmail) {
      UserCLIManager.getUserFromEmail(username)!!.username
    } else {
      username
    }
    // Check serialized members in banlist
    for (uniMember in this.banlist) {
      usernameTmp = Json.decodeFromString<UniMember>(uniMember).username
      if (usernameTmp == usernameToCheck) return true
    }
    return false
  }

  class Connection(val session: DefaultWebSocketSession, val username: String) {
    companion object {
      // var lastId = AtomicInteger(0)
    }
    // val id = "u${lastId.getAndIncrement()}"
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

  /**
   * Entry point for all incoming websocket connections.
   * This function handles all user actions being transmitted via the provided websocket.
   */
  suspend fun DefaultWebSocketServerSession.startSession(appCall: ApplicationCall) {
    val uniChatroomGUID = appCall.parameters["unichatroomGUID"]
    if (uniChatroomGUID.isNullOrEmpty()) {
      appCall.respond(HttpStatusCode.BadRequest)
      return
    }
    val email: String
    var username = ""
    // Wait for bearer token then authorize with provided JWT Token
    try {
      for (frame in incoming) {
        when (frame) {
          is Frame.Text -> {
            // Retrieve username from validated token
            email =
              ServerController.buildJWTVerifier(ServerController.iniVal).verify(frame.readText()).getClaim("username")
                .asString()
            // Further, check user rights
            if (UserCLIManager.checkModuleRight(email = email, module = "M5")) {
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
    // Retrieve Clarifier Session
    val uniChatroom = getOrCreateUniChatroom(uniChatroomGUID, username)
    if (uniChatroom.checkIsMemberBanned(username)) {
      this.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Forbidden"))
      return
    }
    // Add this new WebSocket connection
    val thisConnection = Connection(this, username)
    clarifierSession!!.connections += thisConnection
    if (!uniChatroom.checkIsMember(thisConnection.username)) {
      // Register new member
      uniChatroom.addOrUpdateMember(username, UniRole("Member"))
      saveChatroom(uniChatroom)
      // Notify members of new registration
      val serverNotification = "[s:RegistrationNotification]$username has joined ${uniChatroom.title}!"
      val msg = Json.encodeToString(
              UniMessage(
                      uniChatroomUID = uniChatroom.uID, from = "_server", message = serverNotification
              )
      )
      clarifierSession!!.connections.forEach {
        if (!it.session.outgoing.isClosedForSend) {
          it.session.send(msg)
        }
      }
      mutexMessages.withLock {
        uniChatroom.addMessage(
                member = "_server", message = serverNotification
        )
      }
    }
    // *****************************************
    // Wait for new messages and distribute them
    // *****************************************
    for (frame in incoming) {
      when (frame) {
        is Frame.Text -> {
          // Add new message to the chatroom
          launch {
            val frameText = frame.readText()
            // What's happening?
            if (frameText.startsWith("[c:EDIT<JSON]")) { // Edit Message
              clarifierSession!!.connectionsToDelete.addAll(
                      handleReceivedEditMessage(
                              frameText, "[c:EDIT<JSON]", thisConnection, clarifierSession!!
                      ).connectionsToDelete
              )
            } else if (frameText.startsWith("[c:REACT<JSON]")) { // Reaction
              clarifierSession!!.connectionsToDelete.addAll(
                      handleReceivedReactMessage(
                              frameText, "[c:REACT<JSON]", thisConnection, clarifierSession!!
                      ).connectionsToDelete
              )
            } else if (frameText.startsWith("[c:SC]")) { // Screenshare Blob
              handleReceivedScreenshareBlob(
                      frameText, clarifierSession!!
              )
            } else if (frameText.startsWith("[c:CMD]")) { // Command
              handleReceivedCommand(
                      frameText, clarifierSession!!, uniChatroom
              )
            } else { // Regular Message
              clarifierSession!!.connectionsToDelete.addAll(
                      handleReceivedMessage(
                              frameText, uniChatroom, thisConnection, clarifierSession!!
                      ).connectionsToDelete
              )
            }
            clarifierSession!!.cleanup()
          }
        }

        is Frame.Close -> {
          log(Log.Type.COM, "WS CLOSE FRAME RECEIVED ${call.request.origin.remoteHost}")
          clarifierSession!!.connections.remove(thisConnection)
        }

        else -> {}
      }
    }
  }

  private suspend fun handleReceivedScreenshareBlob(
    frameText: String, clarifierSession: ClarifierSession
  ) {
    clarifierSession.connections.forEach {
      if (!it.session.outgoing.isClosedForSend) {
        it.session.send(frameText)
      } else {
        clarifierSession.connectionsToDelete.add(it)
      }
    }
  }

  private suspend fun handleReceivedEditMessage(
    frameText: String, prefix: String, thisConnection: Connection, clarifierSession: ClarifierSession
  ): ClarifierSession {
    val configJson = frameText.substring(prefix.length)
    // Get payload
    val editMessageConfig: UniChatroomEditMessage = Json.decodeFromString(configJson)
    // Valid?
    if (editMessageConfig.uniMessageGUID.isEmpty()) return clarifierSession
    // Edit message from database
    with(UniMessagesController()) {
      var message: UniMessage? = null
      var chatroomUID = -1
      mutexMessages.withLock {
        getEntriesFromIndexSearch(editMessageConfig.uniMessageGUID, 2, true) {
          message = it as UniMessage
        }
        if (message == null) return clarifierSession
        if (message!!.from != thisConnection.username) return clarifierSession
        chatroomUID = message!!.uniChatroomUID
        message!!.message = editMessageConfig.newContent
        // Did the message get deleted?
        if (message!!.message.isEmpty()) {
          message!!.uniChatroomUID = -1
          message!!.gUID = "?"
        }
        save(message!!)
      }
      val uniMessage = UniMessage(
              uniChatroomUID = chatroomUID, from = "_server", message = "[s:EditNotification]$configJson"
      )
      val msg = Json.encodeToString(uniMessage)
      clarifierSession.connections.forEach {
        if (!it.session.outgoing.isClosedForSend) {
          it.session.send(msg)
        } else {
          clarifierSession.connectionsToDelete.add(it)
        }
      }
    }
    return clarifierSession
  }

  private suspend fun handleReceivedReactMessage(
    frameText: String, prefix: String, thisConnection: Connection, clarifierSession: ClarifierSession
  ): ClarifierSession {
    // Get payload
    val reactMessageConfig: UniChatroomReactMessage = Json.decodeFromString(frameText.substring(prefix.length))
    // Valid?
    if (reactMessageConfig.uniMessageGUID.isEmpty()) return clarifierSession
    // Edit message from database
    with(UniMessagesController()) {
      var message: UniMessage? = null
      var chatroomUID = -1
      // Check if user already reacted to this message in the same way
      // If the user did, remove user, if not, add the user
      var reactionTypeExists = false
      var reactionRemove = false
      mutexMessages.withLock {
        getEntriesFromIndexSearch(reactMessageConfig.uniMessageGUID, 2, true) {
          message = it as UniMessage
        }
        if (message == null) return clarifierSession
        var index = 0
        for (reaction in message!!.reactions) {
          val react: UniMessageReaction
          // Self-healing! If a reaction is broken, just clear it
          try {
            react = Json.decodeFromString(reaction)
          } catch (e: Exception) {
            message!!.reactions[index] = ""
            continue
          }
          if (react.type == reactMessageConfig.type) {
            reactionTypeExists = true
            if (react.from.contains(thisConnection.username)) {
              // Reaction does exist and contains the user -> Return
              reactionRemove = true
              react.from.remove(thisConnection.username)
              if (react.from.size > 0) {
                message!!.reactions[index] = Json.encodeToString(react)
              } else {
                message!!.reactions.removeAt(index)
              }
              break
            } else {
              // Reaction does exist but does not contain the user -> Add user
              react.from.add(thisConnection.username)
              message!!.reactions[index] = Json.encodeToString(react)
              break
            }
          }
          index++
        }
        chatroomUID = message!!.uniChatroomUID
        if (!reactionTypeExists) {
          val reaction = UniMessageReaction(
                  from = arrayListOf(thisConnection.username), type = reactMessageConfig.type
          )
          message!!.reactions.add(Json.encodeToString(reaction))
        }
        save(message!!)
      }
      val response = Json.encodeToString(
              UniChatroomReactMessageResponse(
                      uniMessageGUID = reactMessageConfig.uniMessageGUID,
                      type = reactMessageConfig.type,
                      from = thisConnection.username,
                      isRemove = reactionRemove
              )
      )
      val uniMessage = UniMessage(
              uniChatroomUID = chatroomUID, from = "_server", message = "[s:ReactNotification]$response"
      )
      val msg = Json.encodeToString(uniMessage)
      clarifierSession.connections.forEach {
        if (!it.session.outgoing.isClosedForSend) {
          it.session.send(msg)
        } else {
          clarifierSession.connectionsToDelete.add(it)
        }
      }
    }
    return clarifierSession
  }

  private suspend fun handleReceivedCommand(
    frameText: String, clarifierSession: ClarifierSession, unichatroom: UniChatroom
  ) {
    // What command?
    if (frameText.contains("/help")) {
      val tmpMessage = UniMessage(
              uID = -1,
              uniChatroomUID = -1,
              from = "_server",
              message = "#### List of all commands:\n" + "| Command      | Explanation |" + "\n| ------------ | ----------- |" + "\n| `/gif` [keywords] | Send a random GIF |" + "\n| `/flip` [optional: keywords] | Open the Imgflip interface |" + "\n| `/topflip` [optional: `-d`] | Review the most upvoted image! -d to filter today's images only |" + "\n| `/stats` | View the current subchats' statistics on messages |" + "\n| `/leaderboard` | Show the top members of this subchat |"
      )
      clarifierSession.connections.forEach {
        if (!it.session.outgoing.isClosedForSend) {
          it.session.send(Json.encodeToString(tmpMessage))
        } else {
          clarifierSession.connectionsToDelete.add(it)
        }
      }
    } else if (frameText.contains("/topflip")) {
      if (unichatroom.uID == -1) return
      var isFlipOfTheDay = false
      val dateFrom = Timestamp.now()
      // 2022-07-14T11:06:03Z
      // 00000000001111111111
      // 01234567890123456789
      val year = dateFrom.substring(0, 4).toInt()
      val month = dateFrom.substring(5, 7).toInt()
      val day = dateFrom.substring(8, 10).toInt()
      if (frameText.contains("-d")) {
        isFlipOfTheDay = true
      }
      var topFlip: UniMessage? = null
      var topRating = 0.0
      var ok: Boolean
      uniMessagesIndexManager!!.getEntriesFromIndexSearch(
              searchText = "^${unichatroom.uID}$", ixNr = 1, showAll = true
      ) {
        it as UniMessage
        if (it.message.startsWith("[c:IMG]")) {
          ok = true
          if (isFlipOfTheDay) {
            val flipYear = it.timestamp.substring(0, 4).toInt()
            val flipMonth = it.timestamp.substring(5, 7).toInt()
            val flipDay = it.timestamp.substring(8, 10).toInt()
            ok = flipYear >= year && flipMonth >= month && flipDay >= day
          }
          if (ok) {
            var rating = 0.0
            var upvotes = 0.0
            var downvotes = 0.0
            var stars = 0.0
            if (it.reactions.size > 0) {
              for (reaction in it.reactions) {
                val react: UniMessageReaction = Json.decodeFromString(reaction)
                when (react.type) {
                  "+" -> {
                    upvotes += react.from.size
                  }

                  "⭐" -> {
                    stars += react.from.size
                  }

                  "-" -> {
                    downvotes += react.from.size
                  }
                }
              }
              // Add likes...
              if (upvotes > 0) rating += (1.0 * upvotes)
              // Multiply by stars...
              if (stars > 0) {
                if (rating == 0.0) {
                  rating = 1.0
                }
                rating *= (2.25 * stars)
              }
              // Then subtract downvotes
              if (downvotes > 0) rating -= (1.0 * downvotes)
              // Check if we have found a new topflip!
              if (rating > topRating) {
                topRating = rating
                topFlip = it
              }
            }
          }
        }
      }
      if (topRating > 0.0 && topFlip != null) {
        val prefix = if (isFlipOfTheDay) {
          "The Daily Top Flip..."
        } else "The All-Time Top Flip..."
        val serverMessage = "$prefix Presented by ${topFlip!!.from}!" + "\nRating: $topRating"
        val tmpMessage = UniMessage(
                uID = -1, uniChatroomUID = -1, from = "_server", message = serverMessage
        )
        clarifierSession.connections.forEach {
          if (!it.session.outgoing.isClosedForSend) {
            it.session.send(Json.encodeToString(tmpMessage))
            it.session.send(Json.encodeToString(topFlip))
          } else {
            clarifierSession.connectionsToDelete.add(it)
          }
        }
      } else {
        val tmpMessage = UniMessage(
                uID = -1, uniChatroomUID = -1, from = "_server", message = "No Top Flip Available!"
        )
        clarifierSession.connections.forEach {
          if (!it.session.outgoing.isClosedForSend) {
            it.session.send(Json.encodeToString(tmpMessage))
          } else {
            clarifierSession.connectionsToDelete.add(it)
          }
        }
      }
    } else if (frameText.contains("/stats")) {
      if (unichatroom.uID == -1) return
      var amountMSG = 0
      var amountGIF = 0
      var amountIMG = 0
      var amountAUD = 0
      var amountRCT = 0
      var messagesWithReaction = 0
      uniMessagesIndexManager!!.getEntriesFromIndexSearch(
              searchText = "^${unichatroom.uID}$", ixNr = 1, showAll = true
      ) {
        it as UniMessage
        if (it.from != "_server") {
          // Count each message type
          if (it.message.startsWith("[c:GIF]")) {
            amountGIF += 1
          } else if (it.message.startsWith("[c:IMG]")) {
            amountIMG += 1
          } else if (it.message.startsWith("[c:AUD]")) {
            amountAUD += 1
          } else {
            amountMSG += 1
          }
          // Count reactions!
          if (it.reactions.size > 0) {
            amountRCT += it.reactions.size
            messagesWithReaction += 1
          }
        }
      }
      val messagesTotal = amountGIF + amountIMG + amountMSG + amountAUD
      val messagesWithReactionPercent = (messagesWithReaction.toDouble() / messagesTotal.toDouble()) * 100.0
      val tmpMessage = UniMessage(
              uID = -1,
              uniChatroomUID = -1,
              from = "_server",
              message = "Statistics for this Subchat:\n\nTexts sent: $amountMSG\nGIFs sent: $amountGIF\nImages sent: $amountIMG\nAudios sent: $amountAUD\n\nFor a total of $messagesTotal message(s)\n...with $amountRCT reaction(s)!" + "\n\n${
                messagesWithReactionPercent.roundTo(2)
              }% " + "of all messages received a reaction!"
      )
      clarifierSession.connections.forEach {
        if (!it.session.outgoing.isClosedForSend) {
          it.session.send(Json.encodeToString(tmpMessage))
        } else {
          clarifierSession.connectionsToDelete.add(it)
        }
      }
    } else if (frameText.contains("/leaderboard")) {
      if (unichatroom.uID == -1) return
      val members = getMemberStatsOfChatroom(
              chatroom = getMainChatroom(unichatroom) ?: unichatroom
      )
      // Sort by Rating
      // Negative values to sort descending
      val sortedMembers = members.values.toList().sortedWith(compareBy({ -it.totalRating }, { -it.messages }))
      sortedMembers.forEach { it.totalRating.roundTo(2) }
      val tmpMessage = UniMessage(
              uID = -1,
              uniChatroomUID = -1,
              from = "_server",
              message = "[s:Leaderboard]" + Json.encodeToString(sortedMembers)
      )
      clarifierSession.connections.forEach {
        if (!it.session.outgoing.isClosedForSend) {
          it.session.send(Json.encodeToString(tmpMessage))
        } else {
          clarifierSession.connectionsToDelete.add(it)
        }
      }
    }
  }

  private suspend fun handleReceivedMessage(
    frameText: String, uniChatroom: UniChatroom, thisConnection: Connection, clarifierSession: ClarifierSession
  ): ClarifierSession {
    val prefix = "[c:MSG<ENCR]"
    var isEncrypted = false
    if (frameText.startsWith(prefix)) isEncrypted = true
    val uniMessage = UniMessage(
            uniChatroomUID = uniChatroom.uID, from = thisConnection.username, message = frameText
    )
    uniMessage.isEncrypted = isEncrypted
    val msg = Json.encodeToString(uniMessage)
    clarifierSession.connections.forEach {
      if (!it.session.outgoing.isClosedForSend) {
        it.session.send(msg)
      } else {
        clarifierSession.connectionsToDelete.add(it)
      }
    }
    mutexMessages.withLock {
      uniChatroom.addMessage(thisConnection.username, uniMessage.message, uniMessage.gUID)
    }
    // Send notification to all members
    val fcmTokens: ArrayList<String> = arrayListOf()
    for (memberJson in uniChatroom.members) {
      val member: UniMember = Json.decodeFromString(memberJson)
      if (member.firebaseCloudMessagingToken.isEmpty()) continue
      fcmTokens.add(member.firebaseCloudMessagingToken)
    }
    if (fcmTokens.isNotEmpty()) {/*
       Build the notification
       If there's a parentGUID, then this chatroom must be a subchat
       */
      lateinit var destination: String
      lateinit var subchatGUID: String
      if (uniChatroom.parentGUID.isNotEmpty()) {
        destination = uniChatroom.parentGUID
        subchatGUID = uniChatroom.chatroomGUID
      } else {
        destination = uniChatroom.chatroomGUID
        subchatGUID = ""
      }
      val message = MulticastMessage.builder().setWebpushConfig(
              WebpushConfig.builder().setNotification(
                      WebpushNotification(
                              uniChatroom.title, "${thisConnection.username} has sent a message."
                      )
              ).setFcmOptions(
                      WebpushFcmOptions.withLink("/apps/clarifier/wss/$destination")
              ).putData("dlType", "clarifier").putData("dlDest", "/apps/clarifier/wss/$destination")
                .putData("subchatGUID", subchatGUID).build()
      ).addAllTokens(fcmTokens).build()
      FirebaseMessaging.getInstance().sendMulticast(message)
    }
    return clarifierSession
  }

  private suspend fun getOrCreateUniChatroom(
    uniChatroomGUID: String,
    member: String,
  ): UniChatroom {
    // Does the requested Clarifier Session exist?
    var uniChatroom = getChatroom(uniChatroomGUID)
    if (uniChatroom == null) {
      // Create a new Clarifier Session and join it
      uniChatroom = createChatroom(uniChatroomGUID, "text")
      uniChatroom.addOrUpdateMember(member, UniRole("owner"))
    }
    saveChatroom(uniChatroom)
    return uniChatroom
  }

  /**
   * Adds a Firebase Cloud Messaging Token to this member
   */
  private suspend fun UniMember.subscribeFCM(fcmToken: String) {
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
  private suspend fun UniMember.unsubscribeFCM() {
    this.firebaseCloudMessagingToken = ""
    log(Log.Type.SYS, "User ${this.username} unsubscribed from FCM Push Notifications")
  }

  /**
   * Adds an RSA Public Key in the PEM format to this member
   */
  private fun UniMember.addRSAPubKeyPEM(pubKeyPEM: String) {
    this.pubKeyPEM = pubKeyPEM
  }
}
