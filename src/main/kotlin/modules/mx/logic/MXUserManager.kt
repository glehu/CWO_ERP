package modules.mx.logic

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.IModule
import modules.mx.MXCredentials
import modules.mx.MXUser
import modules.mx.startupRoutines
import tornadofx.Controller
import java.io.File
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class MXUserManager : IModule, Controller()
{
    override fun moduleNameLong() = "MXPasswordManager"
    override fun module() = "MX"

    fun login(username: String, password: String): Boolean
    {
        return compareCredentials(username, password, getCredentials())
    }

    fun getUser(username: String): MXUser?
    {
        return getCredentials().credentials[username]
    }

    fun updateUser(userNew: MXUser, userOriginal: MXUser, credentials: MXCredentials)
    {
        //Check if username changed
        if (userNew.username != userOriginal.username)
        {
            //Username changed => Recreate user entry in map since keys are constant
            credentials.credentials.remove(userOriginal.username)
        }
        credentials.credentials[userNew.username] = userNew
        writeCredentials(credentials)
    }

    fun deleteUser(user: MXUser, credentials: MXCredentials)
    {
        credentials.credentials.remove(user.username)
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
        MXLog.log("MX", MXLog.LogType.INFO, "Credentials updated", moduleNameLong())
    }

    private fun getCredentialsFile() = File("${getModulePath("MX")}\\credentials.dat")

    private fun compareCredentials(username: String, password: String, credentials: MXCredentials): Boolean
    {
        var successful = false
        val user = credentials.credentials[username]
        if (user != null && user.password == encrypt(password, token))
        {
            successful = true
            startupRoutines(user)
            MXLog.log(
                "MX", MXLog.LogType.INFO, "User \"$username\" login successful", moduleNameLong()
            )
        } else MXLog.log(
            "MX", MXLog.LogType.WARNING, "User \"$username\" login failed: wrong credentials", moduleNameLong()
        )
        return successful
    }

    private fun initializeCredentials(credentialsFile: File)
    {
        val user = MXUser("admin", encrypt("admin", token))
        startupRoutines(user)
        credentialsFile.createNewFile()
        user.canAccessMX = true
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