package modules.m2.logic

import api.logic.core.ServerController
import api.misc.json.FriendRequestResponse
import api.misc.json.UniChatroomCreateChatroom
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.Contact
import modules.m5.logic.UniChatroomController
import modules.m7knowledge.Knowledge
import modules.m8notification.Notification
import modules.m8notification.logic.NotificationController
import modules.mx.contactIndexManager
import modules.mx.logic.UserCLIManager

@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
class ContactController : IModule {
  override val moduleNameLong = "ContactController"
  override val module = "M2"
  override fun getIndexManager(): IIndexManager {
    return contactIndexManager!!
  }

  companion object {
    val mutex = Mutex()
  }

  private suspend fun saveEntry(knowledge: Knowledge): Int {
    var uID: Int
    mutex.withLock {
      uID = save(knowledge)
    }
    return uID
  }

  suspend fun httpSendFriendRequest(appCall: ApplicationCall, username: String) {
    var user: Contact? = null
    contactIndexManager!!.getEntriesFromIndexSearch("^$username$", 2, true) {
      it as Contact
      user = it
    }
    if (user == null) {
      val response = FriendRequestResponse(false, "User does not exist.")
      appCall.respond(response)
      return
    }
    // Check if they are friends already
    val chatrooms = UniChatroomController().getDirectChatrooms(appCall, username)
    if (chatrooms.chatrooms.isNotEmpty()) {
      val response = FriendRequestResponse(false, "Friends already.")
      appCall.respond(response)
      return
    }
    // Create Direct Chatroom with both parties
    val usernameTokenEmail = ServerController.getJWTEmail(appCall)
    val usernameToken = UserCLIManager.getUserFromEmail(usernameTokenEmail)!!.username
    val config = UniChatroomCreateChatroom(
            title = "", type = "direct", directMessageUsernames = arrayOf(usernameToken, username)
    )
    val chatroom = UniChatroomController().createConfiguredChatroom(config, usernameToken)
    // Add a notification for the user to be befriended
    val notificationController = NotificationController()
    var notification = Notification(-1, username)
    notification.title = "Friend Request"
    notification.authorUsername = "_server"
    notification.description = "$usernameToken has sent a Friend Request! Click to accept!"
    notification.hasClickAction = true
    notification.clickAction = "join,group"
    notification.clickActionReferenceGUID = chatroom.chatroomGUID
    notificationController.saveEntry(notification)
    // Add a notification for the user that sent the friend request
    notification = Notification(-1, usernameToken)
    notification.title = "Request Sent"
    notification.authorUsername = "_server"
    notification.description = "$username has received your friend request. Waiting for approval."
    notificationController.saveEntry(notification)
    // Respond
    val response = FriendRequestResponse(true, "Friend request sent.")
    appCall.respond(response)
  }
}
