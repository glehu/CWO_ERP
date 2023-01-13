package modules.m8notification.logic

import api.logic.core.ServerController
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
import modules.m8notification.Notification
import modules.mx.logic.Timestamp
import modules.mx.logic.UserCLIManager
import modules.mx.notificationIndexManager

@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
class NotificationController : IModule {
  override val moduleNameLong = "NotificationController"
  override val module = "M8NOTIFICATION"
  override fun getIndexManager(): IIndexManager {
    return notificationIndexManager!!
  }

  companion object {
    val mutex = Mutex()
  }

  suspend fun saveEntry(notification: Notification): Int {
    var uID: Int
    mutex.withLock {
      uID = save(notification)
    }
    return uID
  }

  suspend fun httpGetNotifications(appCall: ApplicationCall) {
    val usernameTokenEmail = ServerController.getJWTEmail(appCall)
    val usernameToken = UserCLIManager.getUserFromEmail(usernameTokenEmail)!!.username
    val notifications = arrayListOf<Notification>()
    mutex.withLock {
      getEntriesFromIndexSearch("^$usernameToken$", 2, true) {
        it as Notification
        notifications.add(it)
      }
    }
    appCall.respond(notifications)
  }

  suspend fun httpDismissNotification(appCall: ApplicationCall, notificationGUID: String) {
    val usernameTokenEmail = ServerController.getJWTEmail(appCall)
    val usernameToken = UserCLIManager.getUserFromEmail(usernameTokenEmail)!!.username
    var notification: Notification? = null
    mutex.withLock {
      getEntriesFromIndexSearch("^$notificationGUID$", 1, true) {
        it as Notification
        notification = it
      }
      if (notification == null) {
        appCall.respond(HttpStatusCode.NotFound)
        return
      }
      if (notification!!.recipientUsername != usernameToken) {
        appCall.respond(HttpStatusCode.Forbidden)
        return
      }
      notification!!.finished = true
      notification!!.finishedDate = Timestamp.now()
      notification!!.recipientUsername = "?"
      save(notification!!)
    }
    appCall.respond(HttpStatusCode.OK)
  }
}
