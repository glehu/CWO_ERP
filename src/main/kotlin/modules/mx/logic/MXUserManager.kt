package modules.mx.logic

import api.logic.getCWOClient
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
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@InternalAPI
@ExperimentalSerializationApi
class MXUserManager : IModule, Controller() {
    override val moduleNameLong = "MXPasswordManager"
    override val module = "MX"
    override fun getIndexManager(): IIndexManager? {
        return null
    }

    fun login(username: String, password: String): Boolean {
        return if (!isClientGlobal) {
            compareCredentials(username, password, getCredentials())
        } else {
            compareCredentialsServer(username, password)
        }
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

    fun getCredentials(): MXCredentials {
        val credentialsFile = getCredentialsFile()
        if (!credentialsFile.isFile) initializeCredentials(credentialsFile)
        return Json.decodeFromString(credentialsFile.readText())
    }

    private fun writeCredentials(credentials: MXCredentials) {
        getCredentialsFile().writeText(Json.encodeToString(credentials))
        log(MXLog.LogType.INFO, "Credentials updated")
    }

    private fun getCredentialsFile() = File("${getModulePath(module)}\\credentials.dat")

    private fun compareCredentials(username: String, password: String, credentials: MXCredentials): Boolean {
        var successful = false
        val user = credentials.credentials[username]
        if (user != null && user.password == encryptAES(password)) {
            successful = true
            if (activeUser.username.isEmpty()) activeUser = user
            if (!isClientGlobal) {
                log(MXLog.LogType.INFO, "User \"$username\" login successful")
            } else {
                log(MXLog.LogType.COM, "User \"$username\" login successful")
            }
        } else log(MXLog.LogType.WARNING, "User \"$username\" login failed: wrong credentials")
        return successful
    }

    /**
     * Compares the credentials the user is trying to log in with the internal credentials' database.
     * @return true if the credentials match with the databases credentials.
     */
    private fun compareCredentialsServer(username: String, password: String): Boolean {
        var successful = false
        val client = getCWOClient(username, password)
        runBlocking {
            launch {
                val response: ValidationContainerJson = client.get("${getServerUrl()}login")
                val loginResponse = Json.decodeFromString<LoginResponseJson>(response.contentJson)
                if (loginResponse.httpCode == 200) {
                    if (validateKeccak(
                            input = response.contentJson,
                            base64KeccakString = response.hash,
                            salt = encryptKeccak(username),
                            pepper = "CWO_ERP LoginValidation"
                        )) {
                        successful = true
                        activeUser = MXUser(username, password)
                        activeUser.canAccessM1 = loginResponse.accessM1
                        activeUser.canAccessM2 = loginResponse.accessM2
                        activeUser.canAccessM3 = loginResponse.accessM3
                    }
                }
            }
        }
        return successful
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

    fun getUsers(users: ObservableList<MXUser>, credentials: MXCredentials): ObservableList<MXUser> {
        users.clear()
        for ((_, v) in credentials.credentials) users.add(v)
        return users
    }

    fun showUser(user: MXUser, credentials: MXCredentials, users: ObservableList<MXUser>) {
        MGXUser(user, credentials).openModal(block = true)
        getUsers(users, credentials)
    }
}