package modules.mx

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.IModule
import tornadofx.Controller
import java.io.File

class MXPasswordManager: IModule, Controller()
{
    override fun moduleName() = "MXPasswordManager"

    fun login(username: String, password: String): Boolean
    {
        return compareCredentials(username, password, checkCredentialsFile())
    }

    private fun compareCredentials(username: String, password: String, credentials: MXCredentials): Boolean
    {
        val user = credentials.credentials[username]
        return user!!.password == password
    }

    private fun checkCredentialsFile(): MXCredentials
    {
        val credentialsFile = File("${getProgramPath()}\\credentials.dat")
        if (!credentialsFile.isFile) initializeCredentials(credentialsFile)
        return Json.decodeFromString(credentialsFile.readText())
    }

    private fun initializeCredentials(credentialsFile: File)
    {
        credentialsFile.createNewFile()
        val user = MXUser("test", "test")
        val credentials = MXCredentials(CredentialsType.MAIN)
        credentials.credentials[user.username] = user
        credentialsFile.writeText(Json.encodeToString(credentials))
    }

    enum class CredentialsType
    {
        MAIN
    }
}