package modules.mx.logic

import api.logic.getTokenClient
import api.logic.getUserClient
import api.misc.json.CWOAuthCallbackJson
import api.misc.json.LoginResponseJson
import api.misc.json.ValidationContainerJson
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.client.request.*
import io.ktor.util.*
import javafx.collections.ObservableList
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.*
import modules.mx.gui.MGXUser
import tornadofx.Controller
import tornadofx.MultiValue
import tornadofx.observableListOf
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@InternalAPI
@ExperimentalSerializationApi
class MXUserManager : IModule, Controller() {
    override val moduleNameLong = "MXUserManager"
    override val module = "MX"
    override fun getIndexManager(): IIndexManager? {
        return null
    }

    /**
     * Attempts to log in a user.
     * @return true if the user is logged in.
     */
    fun login(username: String, password: String, doLog: Boolean = true): Boolean {
        return if (!isClientGlobal) {
            compareCredentials(username, password, getCredentials(), doLog)
        } else {
            compareCredentialsServer(username, password)
        }
    }

    /**
     * Logs out a user.
     */
    fun logout(username: String, password: String) {
        if (!isClientGlobal) {
            setUserOnlineStatus(username, false)
        } else {
            val client = getUserClient(username, password)
            runBlocking {
                launch {
                    client.get("${getServerUrl()}logout")
                }
            }
        }
    }

    fun checkModuleRight(username: String, module: String): Boolean {
        val user = getCredentials().credentials[username]!!
        when (module.uppercase()) {
            "MX" -> return user.canAccessMX
            "M1" -> return user.canAccessM1
            "M2" -> return user.canAccessM2
            "M3" -> return user.canAccessM3
            "M4" -> return user.canAccessM4
        }
        return false
    }

    fun updateUser(userNew: MXUser, userOriginal: MXUser, credentials: MXCredentials) {
        //Check if username changed
        if (userNew.username != userOriginal.username) {
            //Username changed => Recreate user entry in map since keys are constant
            credentials.credentials.remove(userOriginal.username)
        }
        credentials.credentials[userNew.username] = userNew
        writeCredentials(credentials)
    }

    fun deleteUser(user: MXUser, credentials: MXCredentials) {
        credentials.credentials.remove(user.username)
        writeCredentials(credentials)
    }

    fun setUserOnlineStatus(username: String, online: Boolean) {
        val credentials = getCredentials()
        credentials.credentials[username]!!.online = online
        credentials.credentials[username]!!.onlineSince = if (online) {
            LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        } else "?"
        writeCredentials(credentials)
    }

    fun getCredentials(): MXCredentials {
        val credentialsFile = getCredentialsFile()
        if (!credentialsFile.isFile) initializeCredentials(credentialsFile)
        return Json.decodeFromString(credentialsFile.readText())
    }

    private fun writeCredentials(credentials: MXCredentials) {
        getCredentialsFile().writeText(json(true).encodeToString(credentials))
        log(MXLog.LogType.INFO, "Credentials updated")
    }

    private fun getCredentialsFile() = File("${getModulePath(module)}\\credentials.dat")

    private fun compareCredentials(
        username: String,
        password: String,
        credentials: MXCredentials,
        doLog: Boolean
    ): Boolean {
        var successful = false
        val user = credentials.credentials[username]
        if (user != null && user.password == encryptAES(password)) {
            successful = true
            if (activeUser.username.isEmpty()) activeUser = user
            if (doLog) log(MXLog.LogType.INFO, "User \"$username\" login successful")
        } else {
            if (doLog) log(MXLog.LogType.WARNING, "User \"$username\" login failed: wrong credentials")
        }
        return successful
    }

    /**
     * Compares the credentials the user is trying to log in with the internal credentials' database.
     * @return true if the credentials match with the databases credentials.
     */
    private fun compareCredentialsServer(username: String, password: String): Boolean {
        var validResponse = true
        val client = getUserClient(username, password)
        runBlocking {
            launch {
                val response: ValidationContainerJson = client.get("${getServerUrl()}login")
                val loginResponse = Json.decodeFromString<LoginResponseJson>(response.contentJson)
                if (loginResponse.httpCode == 200) {
                    if (validateKeccak(
                            input = response.contentJson,
                            base64KeccakString = response.hash,
                            salt = encryptKeccak(username),
                            pepper = encryptKeccak("CWO_ERP LoginValidation")
                        )) {
                        activeUser = MXUser(username, password)
                        activeUser.apiToken = CWOAuthCallbackJson(
                            accessToken = loginResponse.token,
                            expiresInSeconds = (loginResponse.expiresInMs / 1000) - 5
                        )
                        activeUser.apiToken.initialize()
                        activeUser.canAccessM1 = loginResponse.accessM1
                        activeUser.canAccessM2 = loginResponse.accessM2
                        activeUser.canAccessM3 = loginResponse.accessM3
                        activeUser.canAccessM4 = loginResponse.accessM4
                        val resp: String = getTokenClient().get("${getServerUrl()}tokenremainingtime")
                        println(resp)
                    } else validResponse = false
                }
            }
        }
        return validResponse
    }

    private fun initializeCredentials(credentialsFile: File) {
        val user = MXUser("admin", encryptAES("admin"))
        //startupRoutines()
        credentialsFile.createNewFile()
        user.canAccessMX = true
        val credentials = MXCredentials(CredentialsType.MAIN)
        credentials.credentials[user.username] = user
        credentialsFile.writeText(Json.encodeToString(credentials))
    }

    enum class CredentialsType {
        MAIN
    }

    fun getRightsCellColor(hasRight: Boolean): MultiValue<Paint> =
        if (hasRight) MultiValue(arrayOf(Color.GREEN)) else MultiValue(arrayOf(Color.RED))

    fun addUser(credentials: MXCredentials, users: ObservableList<MXUser>) =
        showUser(MXUser("", ""), credentials, users)

    /**
     * Retrieves all users and puts them in an observable list.
     * if onlineOnline is true, filters users that are offline.
     * @return an observable list of users.
     */
    fun getUsersObservableList(
        users: ObservableList<MXUser>,
        credentials: MXCredentials,
        onlineOnly: Boolean = false
    ): ObservableList<MXUser> {
        users.clear()
        for ((_, user) in credentials.credentials) {
            if (!onlineOnly || (onlineOnly && user.online)) {
                if (user.username != "") users.add(user)
            }
        }
        return users
    }

    fun showUser(user: MXUser, credentials: MXCredentials, users: ObservableList<MXUser>) {
        MGXUser(user, credentials).openModal(block = true)
        getUsersObservableList(users, credentials)
    }

    fun getActiveUsers(): ObservableList<MXUser> {
        return getUsersObservableList(
            users = observableListOf(MXUser("", "")),
            credentials = getCredentials(),
            onlineOnly = true
        )
    }
}
