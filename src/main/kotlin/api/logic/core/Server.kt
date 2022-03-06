package api.logic.core

import api.gui.GSpotify
import api.logic.SpotifyAUTH
import api.logic.webapps.WebPlanner
import api.misc.json.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.network.tls.certificates.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.m4.logic.ItemPriceManager
import modules.m4storage.logic.ItemStorageManager
import modules.mx.*
import modules.mx.gui.GDashboard
import modules.mx.logic.Log
import modules.mx.logic.UserCLIManager
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths

@DelicateCoroutinesApi
@InternalAPI
@ExperimentalSerializationApi
class Server : IModule {
  override val moduleNameLong = "Server"
  override val module = "MX"
  override fun getIndexManager(): IIndexManager? {
    return null
  }

  private val iniVal = Json.decodeFromString<Ini>(getIniFile().readText())
  private val userCLIManager = UserCLIManager()
  lateinit var text: String

  private val keyStoreFile = File(Paths.get("build", "keystore.jks").toString())
  private val keystore = generateCertificate(
    file = keyStoreFile,
    keyAlias = "cWoErP",
    keyPassword = "CwOeRp",
    jksPassword = "CwOeRp"
  )
  private val environment = applicationEngineEnvironment {
    log = LoggerFactory.getLogger("ktor.application")
    connector {
      host = iniVal.serverIPAddress.substringBefore(':')
      port = iniVal.serverIPAddress.substringAfter(':').toInt() + 1
    }
    sslConnector(
      keyStore = keystore,
      keyAlias = "sampleAlias",
      keyStorePassword = { "foobar".toCharArray() },
      privateKeyPassword = { "foobar".toCharArray() }) {
      host = iniVal.serverIPAddress.substringBefore(':')
      port = iniVal.serverIPAddress.substringAfter(':').toInt()
      keyStorePath = keyStoreFile
    }
    module {
      module()
    }
  }

  val serverEngine = embeddedServer(Netty, environment)

  fun Application.module() {
    install(ContentNegotiation) {
      json(Json {
        prettyPrint = true
        isLenient = true
      })
    }
    install(Authentication) {
      basic("auth-basic") {
        realm = "Access to the '/' path"
        validate { credentials ->
          if (userCLIManager.login(credentials.name, credentials.password, false)) {
            UserIdPrincipal(credentials.name)
          } else {
            null
          }
        }
      }
      jwt("auth-jwt") {
        realm = "Access to the '/' path"
        verifier(
          JWT
            .require(Algorithm.HMAC256(iniVal.token))
            .withAudience("https://${iniVal.serverIPAddress}/")
            .withIssuer("https://${iniVal.serverIPAddress}/")
            .build()
        )
        validate { credential ->
          if (credential.payload.getClaim("username").asString() != "") {
            JWTPrincipal(credential.payload)
          } else {
            null
          }
        }
      }
    }
    install(CORS) {
      anyHost()
      header(HttpHeaders.ContentType)
      header(HttpHeaders.Authorization)
    }
    routing {
      route("/") {
        get {
          call.respondFile(File(Paths.get(dataPath, "data", "web", "home.html").toString()))
        }
      }
      route("/web") {
        get {
          call.respondRedirect("https://orochi.netlify.app/")
        }
      }
      route("/mockingbird") {
        post {
          runBlocking {
            val text: String = call.receive()
            delay(text.toLong())
            log(Log.LogType.COM, text, call.request.uri)
            call.respondText(text)
          }
        }
      }
      register()
      spotifyAuthCallback()

      authenticate("auth-basic") {
        login()
        logout()
        //------------------------------------------------------v
        //------------ CWO API, now with JWT AUTH! -------------|
        //------------------------------------------------------^
      }
      authenticate("auth-jwt") {
        tokenRemainingTime()
        route("/api")
        {
          /**
           * General Endpoints
           */
          getIndexSelection(
            discographyIndexManager!!,
            contactIndexManager!!,
            invoiceIndexManager!!,
            itemIndexManager!!,
            itemStockPostingIndexManager!!
          )
          getEntry(
            discographyIndexManager!!,
            contactIndexManager!!,
            invoiceIndexManager!!,
            itemIndexManager!!,
            itemStockPostingIndexManager!!
          )
          saveEntry(
            discographyIndexManager!!,
            contactIndexManager!!,
            invoiceIndexManager!!,
            itemIndexManager!!,
            itemStockPostingIndexManager!!
          )
          getEntryLock(
            discographyIndexManager!!,
            contactIndexManager!!,
            invoiceIndexManager!!,
            itemIndexManager!!,
            itemStockPostingIndexManager!!
          )
          setEntryLock(
            discographyIndexManager!!,
            contactIndexManager!!,
            invoiceIndexManager!!,
            itemIndexManager!!,
            itemStockPostingIndexManager!!
          )
          sendEMail()
          getSettingsFileText()

          /**
           * M3 Endpoints (Invoice)
           */
          getOwnInvoices()

          /**
           * M4 Endpoints (Item)
           */
          getPriceCategories()
          getPriceCategoryNumber()
          savePriceCategory()
          deletePriceCategory()

          getStorages()
          getStorageNumber()
          saveStorage()
          deleteStorage()
          checkStorage()
          getAvailableStock()

          getItemImage()

          /**
           * Web Solution Endpoints
           */
          addWebshopOrder()
          userTracking()
          // Web Apps
          webPlannerCommit()
          webPlannerRequest()
        }
      }
    }
  }

  init {
    serverJobGlobal = GlobalScope.launch {
      serverEngine.start(wait = true)
    }
  }

  private fun Route.logout() {
    get("/logout") {
      userCLIManager.setUserOnlineStatus(
        username = call.principal<UserIdPrincipal>()?.name!!,
        online = false
      )
      log(
        Log.LogType.COM,
        "User ${call.principal<UserIdPrincipal>()?.name} logout",
        call.request.uri
      )
      if (!cliMode) {
        GDashboard().update()
      }
    }
  }

  private fun Route.login() {
    get("/login") {
      log(
        Log.LogType.COM,
        "User ${call.principal<UserIdPrincipal>()?.name} login",
        call.request.uri
      )
      userCLIManager.setUserOnlineStatus(
        username = call.principal<UserIdPrincipal>()?.name!!,
        online = true
      )
      call.respond(
        ServerController.generateLoginResponse(
          userCLIManager
            .getCredentials()
            .credentials[call.principal<UserIdPrincipal>()?.name]!!
        )
      )
      if (!cliMode) {
        GDashboard().update()
      }
    }
  }

  private fun Route.spotifyAuthCallback() {
    get("/authcallback/spotify") {
      val code: String? = call.request.queryParameters["code"]
      if (code != null) {
        call.respondFile(File(Paths.get(dataPath, "data", "web", "spotifyCallback.html").toString()))
        log(Log.LogType.COM, "Spotify Auth Callback received")
        val spotifyAPI = GSpotify()
        spotifyAPI.authCodeProperty.value = code
        spotifyAPI.showTokenData(
          SpotifyAUTH().getAccessTokenFromAuthCode(code) as SpotifyAuthCallbackJson
        )
        spotifyAPI.updateUserData()
      }
    }
  }

  private fun Route.tokenRemainingTime() {
    get("/tokenremainingtime") {
      val principal = call.principal<JWTPrincipal>()
      val expiresInMs = principal!!.expiresAt?.time?.minus(System.currentTimeMillis())
      call.respondText(expiresInMs.toString())
    }
  }

  private fun Route.getOwnInvoices() {
    get("m3/owninvoices") {
      call.respond(ServerController.getOwnInvoices(call))
    }
  }

  private fun Route.userTracking() {
    post("utr") {
      call.respond(usageTracker!!.writeUsageTrackingData(call))
    }
  }

  private fun Route.register() {
    post("register") {
      call.respond(ServerController.registerUser(call))
    }
  }

  private fun Route.getIndexSelection(vararg indexManager: IIndexManager) {
    for (ix in indexManager) {
      get("${ix.module.lowercase()}/indexselection") {
        if (!userCLIManager.checkModuleRight(ServerController.getJWTUsername(call), ix.module)) {
          call.respond(HttpStatusCode.Forbidden)
        } else {
          call.respond(ix.getIndexUserSelection())
        }
      }
    }
  }

  private fun Route.getEntry(vararg indexManager: IIndexManager) {
    for (ix in indexManager) {
      get("${ix.module.lowercase()}/entry/{searchString}") {
        if (!userCLIManager.checkModuleRight(ServerController.getJWTUsername(call), ix.module)) {
          call.respond(HttpStatusCode.Forbidden)
        } else {
          call.respond(
            ServerController.getEntry(
              appCall = call,
              indexManager = ix
            )
          )
        }
      }
    }
  }

  private fun Route.saveEntry(vararg indexManager: IIndexManager) {
    for (ix in indexManager) {
      post("${ix.module.lowercase()}/saveentry") {
        if (!userCLIManager.checkModuleRight(ServerController.getJWTUsername(call), ix.module)) {
          call.respond(HttpStatusCode.Forbidden)
        } else {
          val entryJson: EntryJson = call.receive()
          call.respond(
            ServerController.saveEntry(
              entry = entryJson.entry,
              indexManager = ix,
              username = ServerController.getJWTUsername(call)
            )
          )
        }
      }
    }
  }

  private fun Route.getEntryLock(vararg indexManager: IIndexManager) {
    for (ix in indexManager) {
      get("${ix.module.lowercase()}/getentrylock/{searchString}") {
        if (!userCLIManager.checkModuleRight(ServerController.getJWTUsername(call), ix.module)) {
          call.respond(HttpStatusCode.Forbidden)
        } else {
          call.respond(
            ServerController.getEntryLock(
              appCall = call,
              indexManager = ix
            )
          )
        }
      }
    }
  }

  private fun Route.setEntryLock(vararg indexManager: IIndexManager) {
    for (ix in indexManager) {
      get("${ix.module.lowercase()}/setentrylock/{searchString}") {
        if (!userCLIManager.checkModuleRight(ServerController.getJWTUsername(call), ix.module)) {
          call.respond(HttpStatusCode.Forbidden)
        } else {
          call.respond(
            ServerController.setEntryLock(
              appCall = call,
              indexManager = ix
            )
          )
        }
      }
    }
  }

  private fun Route.addWebshopOrder() {
    post("m3/neworder") {
      if (!userCLIManager.checkModuleRight(ServerController.getJWTUsername(call), "M3")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.placeWebshopOrder(call))
      }
    }
  }

  private fun Route.webPlannerCommit() {
    post("/planner") {
      WebPlanner().save(call)
      call.respond(HttpStatusCode.OK)
    }
  }

  private fun Route.webPlannerRequest() {
    post("/planner/load") {
      call.respond(WebPlanner().load(call))
    }
  }

  private fun Route.getPriceCategories() {
    get("m4/pricecategories") {
      if (!userCLIManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(
          ItemPriceManager().getCategories()
        )
      }
    }
  }

  private fun Route.getPriceCategoryNumber() {
    get("m4/categorynumber") {
      if (!userCLIManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(
          ItemPriceManager().getNumber(ItemPriceManager().getCategories())
        )
      }
    }
  }

  private fun Route.savePriceCategory() {
    post("m4/savecategory") {
      if (!userCLIManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.updatePriceCategories(call.receive() as ListDeltaJson))
      }
    }
  }

  private fun Route.deletePriceCategory() {
    post("m4/deletecategory") {
      if (!userCLIManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.deletePriceCategory(call.receive() as ListDeltaJson))
      }
    }
  }

  private fun Route.getStorages() {
    get("m4/storages") {
      if (!userCLIManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(
          ItemStorageManager().getStorages()
        )
      }
    }
  }

  private fun Route.getStorageNumber() {
    get("m4/storagenumber") {
      if (!userCLIManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(
          ItemStorageManager().getNumber(ItemStorageManager().getStorages())
        )
      }
    }
  }

  private fun Route.saveStorage() {
    post("m4/savestorage") {
      if (!userCLIManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.updateStorages(call.receive() as ListDeltaJson))
      }
    }
  }

  private fun Route.deleteStorage() {
    post("m4/deletestorage") {
      if (!userCLIManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.deleteStorage(call.receive() as ListDeltaJson))
      }
    }
  }

  private fun Route.getItemImage() {
    get("m4/getimage/{itemUID}") {
      if (!userCLIManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.getItemImage())
      }
    }
  }

  private fun Route.checkStorage() {
    post("m4sp/check") {
      if (!userCLIManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.checkStorage(call.receive() as TwoIntOneDoubleJson))
      }
    }
  }

  private fun Route.getAvailableStock() {
    post("m4sp/avail") {
      if (!userCLIManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.getAvailableStock(call.receive() as PairIntJson))
      }
    }
  }

  private fun Route.sendEMail() {
    //TODO: Needs to check rights (currently there are no email rights)
    post("sendemail") {
      val body = call.receive<EMailJson>()
      sendEmail(body.subject, body.body, body.recipient)
      call.respond(true)
    }
  }

  private fun Route.getSettingsFileText() {
    post("getsettingsfiletext") {
      val body: SettingsRequestJson = Json.decodeFromString(call.receive())
      if (!userCLIManager.checkModuleRight(ServerController.getJWTUsername(call), body.module)) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(
          ServerController.getSettingsFileText(
            moduleShort = body.module,
            subSetting = body.subSetting
          )
        )
      }
    }
  }
}
