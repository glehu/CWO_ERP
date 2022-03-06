package api.logic

import api.misc.json.SpotifyUserProfileJson
import interfaces.IAPI
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.getModulePath
import tornadofx.Controller
import java.io.File
import java.nio.file.Paths

@InternalAPI
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
    val path = Paths.get(getModulePath("MX"), "api", apiName).toString()
    val filePath = File(path)
    if (!filePath.isDirectory) filePath.mkdirs()
    val userDataFile = File(Paths.get(path, "${apiName}_user.json").toString())
    if (!userDataFile.isFile) userDataFile.createNewFile()
    return userDataFile
  }

  fun saveUserData(userData: SpotifyUserProfileJson) {
    getUserDataFile().writeText(Json.encodeToString(userData))
  }
}
