package modules.mx.logic

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.IModule
import modules.mx.getModulePath
import modules.mx.getToken
import modules.mx.misc.MXCredentials
import modules.mx.misc.MXUser
import tornadofx.Controller
import java.io.File
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class MXPasswordManager : IModule, Controller()
{
    override fun moduleName() = "MXPasswordManager"

    fun login(username: String, password: String): Boolean
    {
        return compareCredentials(username, password, getCredentials())
    }

    fun updateUser(user: MXUser)
    {
        val credentials = getCredentials()
        credentials.credentials[user.username] = user
        writeCredentials(credentials)
    }

    fun getCredentials(): MXCredentials
    {
        val credentialsFile = getCredentialsFile()
        if (!credentialsFile.isFile) initializeCredentials(credentialsFile)
        return Json.decodeFromString(credentialsFile.readText())
    }

    private fun writeCredentials(credentials: MXCredentials)
    {
        getCredentialsFile().writeText(Json.encodeToString(credentials))
        MXLog.log("MX", MXLog.LogType.INFO, "Credentials updated", moduleName())
    }

    private fun getCredentialsFile() = File("${getModulePath("MX")}\\credentials.dat")

    private fun compareCredentials(username: String, password: String, credentials: MXCredentials): Boolean
    {
        var successful = false
        val user = credentials.credentials[username]
        if (user!!.password == encrypt(password, getToken()))
        {
            successful = true
            MXLog.log(
                "MX", MXLog.LogType.INFO, "User \"$username\" login successful", moduleName()
            )
        } else MXLog.log(
            "MX", MXLog.LogType.WARNING, "User \"$username\" login failed: wrong credentials", moduleName()
        )
        return successful
    }

    private fun initializeCredentials(credentialsFile: File)
    {
        credentialsFile.createNewFile()
        val user = MXUser("test", encrypt("test", getToken()))
        val credentials = MXCredentials(CredentialsType.MAIN)
        credentials.credentials[user.username] = user
        credentialsFile.writeText(Json.encodeToString(credentials))
    }

    enum class CredentialsType
    {
        MAIN
    }

    fun encrypt(input: String, token: String): String
    {
        val cipher = Cipher.getInstance("AES")
        val keySpec = SecretKeySpec(token.toByteArray(), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val encrypt = cipher.doFinal(input.toByteArray())
        return Base64.getEncoder().encodeToString(encrypt)
    }

    fun decrypt(input: String, token: String): String
    {
        val cipher = Cipher.getInstance("AES")
        val keySpec = SecretKeySpec(token.toByteArray(), "AES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        val decrypt = cipher.doFinal(Base64.getDecoder().decode(input))
        return String(decrypt)
    }
}