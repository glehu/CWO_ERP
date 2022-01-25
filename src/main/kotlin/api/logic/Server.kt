package api.logic

import api.gui.GSpotify
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
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.m4.logic.ItemPriceManager
import modules.m4storage.logic.ItemStorageManager
import modules.mx.*
import modules.mx.gui.GDashboard
import modules.mx.logic.Log
import modules.mx.logic.UserManager
import tornadofx.Controller
import java.io.File

@DelicateCoroutinesApi
@InternalAPI
@ExperimentalSerializationApi
class Server : IModule, Controller() {
  override val moduleNameLong = "Server"
  override val module = "MX"
  override fun getIndexManager(): IIndexManager? {
    return null
  }

  private val iniVal = Json.decodeFromString<Ini>(getIniFile().readText())
  private val userManager: UserManager by inject()
  lateinit var text: String

  init {
    serverJobGlobal = GlobalScope.launch {
      serverEngine.start(wait = true)
    }
  }

  val serverEngine = embeddedServer(
    factory = Netty,
    host = iniVal.serverIPAddress.substringBefore(':'),
    port = iniVal.serverIPAddress.substringAfter(':').toInt()
  ) {
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
          if (userManager.login(credentials.name, credentials.password, false)) {
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
            .withAudience("http://${iniVal.serverIPAddress}/")
            .withIssuer("http://${iniVal.serverIPAddress}/")
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
          call.respondFile(File("$dataPath\\data\\web\\home.html"))
        }
      }
      route("/web") {
        get {
          call.respondRedirect("https://orochi.netlify.app/")
        }
      }
      route("/mockingbird") {
        post {
          val text: String = call.receive()
          log(Log.LogType.COM, text, call.request.uri)
          call.respondText(text)
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

          getItemImage()

          /**
           * Web Solution Endpoints
           */
          addWebshopOrder()
          userTracking()
        }
      }
    }
  }

  private fun Route.logout() {
    get("/logout") {
      UserManager().setUserOnlineStatus(
        username = call.principal<UserIdPrincipal>()?.name!!,
        online = false
      )
      log(
        Log.LogType.COM,
        "User ${call.principal<UserIdPrincipal>()?.name} logout",
        call.request.uri
      )
      if (!cliMode) {
        find<GDashboard>().activeUsers.items = userManager.getActiveUsers()
        find<GDashboard>().activeUsers.refresh()
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
      val userManager = UserManager()
      userManager.setUserOnlineStatus(
        username = call.principal<UserIdPrincipal>()?.name!!,
        online = true
      )
      call.respond(
        ServerController.generateLoginResponse(
          userManager
            .getCredentials()
            .credentials[call.principal<UserIdPrincipal>()?.name]!!
        )
      )
      if (!cliMode) {
        find<GDashboard>().activeUsers.items = userManager.getActiveUsers()
        find<GDashboard>().activeUsers.refresh()
      }
    }
  }

  private fun Route.spotifyAuthCallback() {
    get("/authcallback/spotify") {
      val code: String? = call.request.queryParameters["code"]
      if (code != null) {
        call.respondFile(File("$dataPath\\data\\web\\spotifyCallback.html"))
        log(Log.LogType.COM, "Spotify Auth Callback received")
        val spotifyAPI = find<GSpotify>()
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
        if (!userManager.checkModuleRight(ServerController.getJWTUsername(call), ix.module)) {
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
        if (!userManager.checkModuleRight(ServerController.getJWTUsername(call), ix.module)) {
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
        if (!userManager.checkModuleRight(ServerController.getJWTUsername(call), ix.module)) {
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
        if (!userManager.checkModuleRight(ServerController.getJWTUsername(call), ix.module)) {
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
        if (!userManager.checkModuleRight(ServerController.getJWTUsername(call), ix.module)) {
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
      if (!userManager.checkModuleRight(ServerController.getJWTUsername(call), "M3")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.placeWebshopOrder(call))
      }
    }
  }

  private fun Route.getPriceCategories() {
    get("m4/pricecategories") {
      if (!userManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
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
      if (!userManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
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
      if (!userManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.updatePriceCategories(call.receive() as ListDeltaJson))
      }
    }
  }

  private fun Route.deletePriceCategory() {
    post("m4/deletecategory") {
      if (!userManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.deletePriceCategory(call.receive() as ListDeltaJson))
      }
    }
  }

  private fun Route.getStorages() {
    get("m4/storages") {
      if (!userManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
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
      if (!userManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
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
      if (!userManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.updateStorages(call.receive() as ListDeltaJson))
      }
    }
  }

  private fun Route.deleteStorage() {
    post("m4/deletestorage") {
      if (!userManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.deleteStorage(call.receive() as ListDeltaJson))
      }
    }
  }

  private fun Route.getItemImage() {
    get("m4/getimage/{itemUID}") {
      if (!userManager.checkModuleRight(ServerController.getJWTUsername(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.getItemImage())
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
      if (!userManager.checkModuleRight(ServerController.getJWTUsername(call), body.module)) {
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
