package api.logic.webapps

import api.logic.core.ServerController
import api.misc.json.MockingbirdConfig
import api.misc.json.WebMockingbirdConfig
import interfaces.IIndexManager
import interfaces.IModule
import interfaces.IWebApp
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.*
import kotlinx.coroutines.delay
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.logic.Log
import java.util.*
import kotlin.system.measureTimeMillis

@ExperimentalSerializationApi
@InternalAPI
class Mockingbird {
  companion object Mockingbird : IWebApp, IModule {
    override val moduleNameLong = "WebMockingbird"
    override val module = "MX"
    override fun getIndexManager(): IIndexManager? {
      return null
    }

    override val webAppName = "Mockingbird"

    suspend fun handleRequest(appCall: ApplicationCall) {
      val who = appCall.request.queryParameters["who"] ?: ""
      if (who.isEmpty()) return
      // Get Config
      val config: MockingbirdConfig
      // Measure time to stay as close as possible to the defined delay time by calculate the delta
      val elapsed = measureTimeMillis {
        config =
          Json.decodeFromString<WebMockingbirdConfig>(getProjectJsonFile(who).readText()).config
      }
      when (config.return_type) {
        "Message" -> respondWithMessage(appCall, config, elapsed)
        "HTTP Code" -> respondWithHTTPCode(appCall, config, elapsed)
        else -> appCall.respond(HttpStatusCode.NotFound)
      }
    }

    private fun respondWithHTTPCode(appCall: ApplicationCall, config: MockingbirdConfig, elapsed: Long) {
    }

    private suspend fun respondWithMessage(appCall: ApplicationCall, config: MockingbirdConfig, elapsed: Long) {
      val message: String
      val delayMs: Long
      // Measure time to stay as close as possible to the defined delay time by calculate the delta
      val elapsedLocal = measureTimeMillis {
        message = when (config.message_type) {
          "Same Message" -> appCall.receive()
          "Fixed Message" -> config.return_message
          else -> appCall.receive()
        }
        delayMs = when (config.return_delay_unit) {
          "Seconds" -> config.return_delay.toLong() * 1000
          "Milliseconds" -> config.return_delay.toLong()
          else -> 0
        }
      }
      // How long do we still have to wait?
      val timeToWait = delayMs - (elapsed + elapsedLocal)
      if (timeToWait > 0) delay(timeToWait)
      appCall.respond(message)
    }

    suspend fun handleSubmit(appCall: ApplicationCall) {
      val type = appCall.request.queryParameters["type"] ?: ""
      if (type.isEmpty()) return
      when (type) {
        "config" -> handleSubmitConfig(appCall)
        "load_config" -> handleLoadConfig(appCall)
      }
    }

    private suspend fun handleLoadConfig(appCall: ApplicationCall) {
      appCall.respond(getProjectJsonFile(getUsernameReversedBase(appCall)).readText())
    }

    private fun getUsernameReversedBase(appCall: ApplicationCall): String {
      val username = ServerController.getJWTUsername(appCall)
      val usernameReversed = username.reversed()
      val usernameBase = Base64.getUrlEncoder().encodeToString(usernameReversed.toByteArray())
      return java.net.URLEncoder.encode(usernameBase, "utf-8")

    }

    private suspend fun handleSubmitConfig(appCall: ApplicationCall) {
      log(Log.LogType.COM, "Mock Config Submission", "/mockingbird/submit")
      val config: WebMockingbirdConfig = appCall.receive()
      // Check Config
      // ...
      getProjectJsonFile(getUsernameReversedBase(appCall)).writeText(Json.encodeToString(config))
      appCall.respond(HttpStatusCode.OK)
    }
  }
}
