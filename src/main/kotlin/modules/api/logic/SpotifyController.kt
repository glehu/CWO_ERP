package modules.api.logic

import interfaces.IAPI
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.api.json.SpotifyUserProfileJson
import modules.mx.getModulePath
import tornadofx.Controller
import java.io.File

@ExperimentalSerializationApi
class SpotifyController : IAPI, Controller() {
    override val apiName = "spotify"
    override val auth = SpotifyAUTH()

    fun getUserData(): SpotifyUserProfileJson {
        val userDataFile = getUserDataFile()
        val fileContent = userDataFile.readText()
        val userData: SpotifyUserProfileJson
        if (fileContent.isEmpty()) {
            if (auth.getAccessAndRefreshTokenFromDisk().accessToken != "?") {
                userData = SpotifyAPI().getAccountData()
                saveUserData(userData)
            } else userData = SpotifyUserProfileJson()
        } else {
            userData = Json.decodeFromString(fileContent)
        }
        return userData
    }

    private fun getUserDataFile(): File {
        val filePath = File("${getModulePath("MX")}\\api\\$apiName")
        if (!filePath.isDirectory) filePath.mkdirs()
        val userDataFile = File("$filePath\\${apiName}_user.json")
        if (!userDataFile.isFile) userDataFile.createNewFile()
        return userDataFile
    }

    fun saveUserData(userData: SpotifyUserProfileJson) {
        getUserDataFile().writeText(Json.encodeToString(userData))
    }
}