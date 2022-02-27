package api.logic.webapps

import api.misc.json.WebPlannerCommit
import api.misc.json.WebPlannerRequest
import api.misc.json.WebPlannerResponse
import interfaces.IIndexManager
import interfaces.IModule
import interfaces.IWebApp
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.logic.Log
import java.io.File

@ExperimentalSerializationApi
@InternalAPI
class WebPlanner : IWebApp, IModule {
  override val moduleNameLong = "WebPlanner"
  override val module = "MX"
  override fun getIndexManager(): IIndexManager? {
    return null
  }

  override val webAppName = "Planner"

  suspend fun save(call: ApplicationCall) {
    val commit = call.receive<WebPlannerCommit>()
    if (commit.action != "save") return
    if (commit.project.isEmpty()) return
    val userDir = commit.project
    checkUserDir(userDir)
    checkUserProject(userDir)
    getUserProjectFile(userDir).writeText(Json.encodeToString(commit.cells))
    log(Log.LogType.COM, "Planner Project $userDir Saved", "/api/planner")
  }

  suspend fun load(call: ApplicationCall): WebPlannerResponse {
    val request = call.receive<WebPlannerRequest>()
    if (request.action != "load") return WebPlannerResponse("load", arrayOf()) //Return an empty array
    if (request.project.isEmpty()) return WebPlannerResponse("load", arrayOf()) //Same as above
    val userDir = request.project
    checkUserDir(userDir)
    checkUserProject(userDir)
    return WebPlannerResponse("load", Json.decodeFromString(getUserProjectFile(userDir).readText()))
  }

  private fun getUserProjectFile(project: String): File {
    return File(getWebAppPath(project) + project + ".json")
  }

  private fun checkUserProject(project: String, createIfMissing: Boolean = true) {
    if (project.isEmpty()) return
    val projectFile = getUserProjectFile(project)
    if (!projectFile.isFile && createIfMissing) projectFile.createNewFile()
  }

  private fun checkUserDir(userDir: String, createIfMissing: Boolean = true) {
    if (userDir.isEmpty()) return
    if (!File(getWebAppPath(userDir)).isDirectory && createIfMissing) File(getWebAppPath(userDir)).mkdirs()
  }
}
