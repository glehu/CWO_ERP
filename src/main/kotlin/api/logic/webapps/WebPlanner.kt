package api.logic.webapps

import api.misc.json.WebPlannerCommit
import api.misc.json.WebPlannerRequest
import api.misc.json.WebPlannerResponse
import interfaces.IIndexManager
import interfaces.IModule
import interfaces.IWebApp
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    getProjectJsonFile(commit.project, commit.project).writeText(Json.encodeToString(commit.cells))
  }

  suspend fun load(call: ApplicationCall): WebPlannerResponse {
    val request = call.receive<WebPlannerRequest>()
    if (request.action != "load" || request.project.isEmpty()) return WebPlannerResponse(
      "load",
      arrayOf()
    ) //Return an empty array
    return WebPlannerResponse(
      "load",
      Json.decodeFromString(getProjectJsonFile(request.project, request.project).readText())
    )
  }
}
