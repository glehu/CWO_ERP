package api.logic.webapps

import api.logic.core.ServerController.Server.getUsernameReversedBase
import api.misc.json.MockingbirdConfig
import api.misc.json.WebMockingbirdConfig
import interfaces.IIndexManager
import interfaces.IModule
import interfaces.IWebApp
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.logic.Log
import kotlin.system.measureTimeMillis

@ExperimentalSerializationApi
@InternalAPI
class Mockingbird {
  @DelicateCoroutinesApi
  @ExperimentalCoroutinesApi
  companion object Mockingbird : IWebApp, IModule {
    override val moduleNameLong = "WebMockingbird"
    override val module = "MX"
    override fun getIndexManager(): IIndexManager? {
      return null
    }

    override val webAppName = "Mockingbird"

    suspend fun handleRequest(appCall: ApplicationCall) {
      val who = appCall.request.queryParameters["who"] ?: ""
      if (who.isEmpty()) {
        appCall.respond(appCall.receive())
        return
      }
      // Get Config
      val config: MockingbirdConfig
      // Measure time to stay as close as possible to the defined delay time by calculate the delta
      val elapsed = measureTimeMillis {
        config =
          Json.decodeFromString<WebMockingbirdConfig>(getProjectJsonFile(who, who).readText()).config
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
      val userString = getUsernameReversedBase(appCall)
      appCall.respond(getProjectJsonFile(userString, userString).readText())
    }

    private suspend fun handleSubmitConfig(appCall: ApplicationCall) {
      log(Log.Type.COM, "Mock Config Submission", "/mockingbird/submit")
      val config: WebMockingbirdConfig = appCall.receive()
      // Check Config
      val userString = getUsernameReversedBase(appCall)
      getProjectJsonFile(userString, userString).writeText(Json.encodeToString(config))
      appCall.respond(HttpStatusCode.OK)
    }
  }
}
