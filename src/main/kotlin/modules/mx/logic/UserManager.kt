package modules.mx.logic

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import javafx.collections.ObservableList
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.Credentials
import modules.mx.User
import modules.mx.gui.GUser
import tornadofx.Controller
import tornadofx.MultiValue
import tornadofx.observableListOf
import kotlin.collections.component1
import kotlin.collections.component2

@InternalAPI
@ExperimentalSerializationApi
class UserManager : IModule, Controller() {
  override val moduleNameLong = "UserManager"
  override val module = "MX"
  override fun getIndexManager(): IIndexManager? {
    return null
  }

  fun getRightsCellColor(hasRight: Boolean): MultiValue<Paint> =
    if (hasRight) MultiValue(arrayOf(Color.GREEN)) else MultiValue(arrayOf(Color.RED))

  fun addUser(credentials: Credentials, users: ObservableList<User>) =
    showUser(User("", ""), credentials, users)

  /**
   * Retrieves all users and puts them in an observable list.
   * if onlineOnline is true, filters users that are offline.
   * @return an observable list of users.
   */
  fun getUsersObservableList(
    users: ObservableList<User>,
    credentials: Credentials,
    onlineOnly: Boolean = false
  ): ObservableList<User> {
    users.clear()
    for ((_, user) in credentials.credentials) {
      if (!onlineOnly || (onlineOnly && user.online)) {
        if (user.username != "") users.add(user)
      }
    }
    return users
  }

  fun showUser(user: User, credentials: Credentials, users: ObservableList<User>) {
    GUser(user, credentials).openModal(block = true)
    getUsersObservableList(users, credentials)
  }

  fun getActiveUsers(): ObservableList<User> {
    return getUsersObservableList(
      users = observableListOf(User("", "")),
      credentials = UserCLIManager().getCredentials(),
      onlineOnly = true
    )
  }
}
