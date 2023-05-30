package api.logic.webapps

import api.logic.core.Connector
import api.logic.core.ServerController
import api.logic.core.ServerController.Server.getUsernameReversedBase
import api.misc.json.ConnectorFrame
import api.misc.json.MockingbirdCallback
import api.misc.json.MockingbirdConfig
import api.misc.json.MockingbirdHeader
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
import modules.mx.logic.Timestamp
import modules.mx.logic.UserCLIManager
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
        config = Json.decodeFromString<WebMockingbirdConfig>(getProjectJsonFile(who, who).readText()).config
      }
      when (config.return_type) {
        "Message" -> respondWithMessage(appCall, config, elapsed)
        "HTTP Code" -> respondWithMessage(appCall, config, elapsed, true)
        else -> appCall.respond(HttpStatusCode.NotFound)
      }
    }

    private suspend fun respondWithMessage(
      appCall: ApplicationCall,
      config: MockingbirdConfig,
      elapsed: Long,
      isHTTPCode: Boolean = false
    ) {
      var message = ""
      var returnCode = HttpStatusCode.OK
      val delayMs: Long
      // Measure time to stay as close as possible to the defined delay time by calculate the delta
      val requestMessage: String
      val elapsedLocal = measureTimeMillis {
        requestMessage = appCall.receive()
        if (!isHTTPCode) {
          message = when (config.message_type) {
            "Same Message" -> requestMessage
            "Fixed Message" -> config.return_message
            else -> ""
          }
        }
        delayMs = when (config.return_delay_unit) {
          "Seconds" -> config.return_delay.toLong() * 1000
          "Milliseconds" -> config.return_delay.toLong()
          else -> 0
        }
        if (config.return_code.isNotEmpty()) {
          returnCode = HttpStatusCode(
                  value = config.return_code.substringBefore(" ").toInt(),
                  description = config.return_code.substringAfter(" "))
        }
      }
      // How long do we still have to wait?
      val timeToWait = delayMs - (elapsed + elapsedLocal)
      if (timeToWait > 0) delay(timeToWait)
      // Send response
      appCall.respond(returnCode, message)
      // Prepare callback
      val headerArray = arrayListOf<MockingbirdHeader>()
      for (headerName in appCall.request.headers.names()) {
        headerArray.add(MockingbirdHeader(headerName, appCall.request.headers[headerName] ?: ""))
      }
      val callbackPayload = MockingbirdCallback(
              requestPayload = requestMessage, requestHeaders = headerArray, responsePayload = message, returnCode.toString())
      if (config.callbackUsername.isNotEmpty()) {
        Connector.sendFrame(
                username = config.callbackUsername, frame = ConnectorFrame(
                type = "mock", msg = "", date = Timestamp.now(), obj = Json.encodeToString(callbackPayload),
                receiveAction = "inc,mock"))
      }
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
      val config: WebMockingbirdConfig = Json.decodeFromString(appCall.receive())
      config.config.callbackUsername = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))!!.username
      // Check Config
      val userString = getUsernameReversedBase(appCall)
      getProjectJsonFile(userString, userString).writeText(Json.encodeToString(config))
      appCall.respond(HttpStatusCode.OK)
    }
  }
}
