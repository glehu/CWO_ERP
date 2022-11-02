package modules.mx.logic

import api.misc.json.PasswordChange
import api.misc.json.UsernameChange
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.Contact
import modules.mx.contactIndexManager

@ExperimentalSerializationApi
@InternalAPI
class UserCLIManager {
  companion object UserManager : IModule {
    override val moduleNameLong = "UserCLIManager"
    override val module = "MX"
    override fun getIndexManager(): IIndexManager? {
      return null
    }

    private val mutex = Mutex()

    /**
     * Attempts to log in a user.
     * @return true if the user is logged in.
     */
    suspend fun login(email: String, password: String, doLog: Boolean = true): Boolean {
      return compareCredentials(email, password, doLog)
    }

    fun checkModuleRight(email: String, module: String): Boolean {
      if (email.isEmpty() || module.length < 2) return false
      val user = getUserFromEmail(email) ?: return false
      when (module.uppercase().substring(0, 2)) {
        "MX" -> return user.canAccessManagement
        "M1" -> return user.canAccessDiscography
        "M2" -> return user.canAccessContacts
        "M3" -> return user.canAccessInvoices
        "M4" -> return user.canAccessInventory
        "M5" -> return user.canAccessClarifier
        "M6" -> return user.canAccessSnippetBase
        "M*" -> {
          var flag = 0
          if (user.canAccessManagement) flag++
          if (user.canAccessDiscography) flag++
          if (user.canAccessContacts) flag++
          if (user.canAccessInvoices) flag++
          if (user.canAccessInventory) flag++
          if (user.canAccessClarifier) flag++
          if (user.canAccessSnippetBase) flag++
          return (flag > 0)
        }

        else -> return false
      }
    }

    private suspend fun compareCredentials(
      email: String, password: String, doLog: Boolean
    ): Boolean {
      mutex.withLock {
        val user = getUserFromEmail(email)
        if (user == null) {
          if (doLog) log(Log.Type.ERROR, "No such user found")
          return false
        }
        return if (user.password == encryptKeccak(password)) {
          if (doLog) log(Log.Type.INFO, "User \"$email\" login successful")
          true
        } else {
          if (doLog) log(Log.Type.ERROR, "User \"$email\" login failed: wrong credentials")
          false
        }
      }
    }

    fun getUserFromEmail(email: String): Contact? {
      var user: Contact? = null
      contactIndexManager!!.getEntriesFromIndexSearch(
              searchText = "^${email}$", ixNr = 1, showAll = true
      ) { user = it as Contact }
      return user
    }

    fun getUserFromUsername(username: String): Contact? {
      var user: Contact? = null
      contactIndexManager!!.getEntriesFromIndexSearch(
              searchText = "^${username}$", ixNr = 2, showAll = true
      ) { user = it as Contact }
      return user
    }

    suspend fun changeUsername(email: String, config: UsernameChange): Boolean {
      val user = getUserFromEmail(email) ?: return false
      user.username = config.newUsername
      save(user)
      return true
    }

    suspend fun changePassword(email: String, config: PasswordChange): Boolean {
      val user = getUserFromEmail(email) ?: return false
      if (!validateKeccak(config.password, user.password)) return false
      user.password = encryptKeccak(config.newPassword)
      save(user)
      return true
    }
  } // END
}
