package api.logic.core

import api.gui.GSpotify
import api.logic.SpotifyAUTH
import api.logic.webapps.Mockingbird
import api.logic.webapps.WebPlanner
import api.misc.json.EMailJson
import api.misc.json.EntryJson
import api.misc.json.FirebaseCloudMessagingSubscription
import api.misc.json.ListDeltaJson
import api.misc.json.PairIntJson
import api.misc.json.SettingsRequestJson
import api.misc.json.SpotifyAuthCallbackJson
import api.misc.json.TwoIntOneDoubleJson
import api.misc.json.UniChatroomAddMember
import api.misc.json.UniChatroomAddMessage
import api.misc.json.UniChatroomCreateChatroom
import api.misc.json.UniChatroomMemberRole
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.m4.logic.ItemPriceManager
import modules.m4storage.logic.ItemStorageManager
import modules.m5.logic.UniChatroomController
import modules.mx.Ini
import modules.mx.cliMode
import modules.mx.contactIndexManager
import modules.mx.dataPath
import modules.mx.discographyIndexManager
import modules.mx.getIniFile
import modules.mx.gui.GDashboard
import modules.mx.invoiceIndexManager
import modules.mx.itemIndexManager
import modules.mx.itemStockPostingIndexManager
import modules.mx.logic.Log
import modules.mx.logic.UserCLIManager
import modules.mx.programPath
import modules.mx.serverJobGlobal
import modules.mx.usageTracker
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.nio.file.Paths
import java.security.KeyStore
import java.time.Duration

@ExperimentalCoroutinesApi
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
  private val keystore: KeyStore = KeyStore.getInstance("JKS")
  private val environment = applicationEngineEnvironment {
    log = LoggerFactory.getLogger("ktor.application")
    connector {
      host = iniVal.serverIPAddress.substringBefore(':')
      port = 80
    }
    sslConnector(
      keyStore = keystore,
      keyAlias = System.getenv(iniVal.envKeyAlias) ?: "?",
      keyStorePassword = { (System.getenv(iniVal.envKeyStorePassword) ?: "?").toCharArray() },
      privateKeyPassword = { (System.getenv(iniVal.envPrivKeyPassword) ?: "?").toCharArray() }
    ) {
      host = iniVal.serverIPAddress.substringBefore(':')
      port = 443
    }
    module {
      module()
    }
  }

  val serverEngine = embeddedServer(Netty, environment)

  fun Application.module() {
    install(HttpsRedirect) {
      sslPort = 443
      permanentRedirect = true
    }
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
        verifier(ServerController.buildJWTVerifier(iniVal))
        // Check if user can access any module
        validate { credential ->
          if (UserCLIManager().checkModuleRight(
              credential.payload.getClaim("username").asString(), "M*"
            )) {
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
    install(WebSockets) {
      pingPeriod = Duration.ofSeconds(2)
      timeout = Duration.ofSeconds(2)
      maxFrameSize = Long.MAX_VALUE
      masking = false
    }
    install(DoubleReceive)
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
        // Mock Requests
        post {
          Mockingbird.handleRequest(call)
        }
      }
      register()
      spotifyAuthCallback()

      /*
       * Clarifier WebSocket Session
      */
      webSocket("/clarifier/{unichatroomGUID}") {
        with(UniChatroomController()) {
          this@webSocket.startSession(call)
        }
      }

      authenticate("auth-basic") {
        login()
        logout()
        //------------------------------------------------------v
        //------------ CWO API, now with JWT AUTH! -------------|
        //------------------------------------------------------^
      }
      authenticate("auth-jwt") {
        tokenRemainingTime()
        // Mockingbird Settings etc.
        route("/mockingbird/submit") {
          post {
            runBlocking {
              ServerController.pauseRequest(2000)
              Mockingbird.handleSubmit(call)
            }
          }
          get {
            Mockingbird.handleSubmit(call)
          }
        }
        route("/api")
        {
          /*
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

          /*
           * M3 Endpoints (Invoice)
           */
          getOwnInvoices()

          /*
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

          /*
           * M5 Endpoints (UniChatroom)
           */
          createUniChatroom()
          getUniChatroom()
          addMessageToUniChatroom()
          getMessagesOfUniChatroom()
          addMemberToUniChatroom()
          getMembersOfUniChatroom()
          addRoleToMemberOfUniChatroom()
          removeRoleOfMemberOfUniChatroom()
          setFirebaseCloudMessagingSubscription()

          /*
           * Web Solution Endpoints
           */
          addWebshopOrder()
          userTracking()
          /*
           * Web Apps
           */
          webPlannerCommit()
          webPlannerRequest()
        }
      }
    }
  }

  init {
    // Get HTTPS Certificate
    keystore.load(
      FileInputStream(Paths.get(programPath, "keystore.jks").toString()),
      (System.getenv(iniVal.envCertPassword) ?: "?").toCharArray()
    )
    // Start Server
    serverJobGlobal = GlobalScope.launch {
      serverEngine.start(wait = true)
    }
    // Initialize Firebase Cloud Messaging Admin SDK
    val options = FirebaseOptions.builder()
      .setCredentials(GoogleCredentials.getApplicationDefault())
      .build()
    FirebaseApp.initializeApp(options)
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

  private fun Route.createUniChatroom() {
    post("m5/createchatroom") {
      val config: UniChatroomCreateChatroom = Json.decodeFromString(call.receive())
      call.respond(ServerController.createUniChatroom(config, ServerController.getJWTUsername(call)))
    }
  }

  private fun Route.getUniChatroom() {
    get("m5/getchatroom/{uniChatroomGUID}") {
      ServerController.getUniChatroom(call)
    }
  }

  private fun Route.addMessageToUniChatroom() {
    post("m5/addmessage") {
      val config: UniChatroomAddMessage = Json.decodeFromString(call.receive())
      ServerController.addMessageToUniChatroom(call, config, ServerController.getJWTUsername(call))
    }
  }

  private fun Route.getMessagesOfUniChatroom() {
    get("m5/getmessages/{uniChatroomGUID}") {
      ServerController.getMessagesOfUniChatroom(call)
    }
  }

  private fun Route.addMemberToUniChatroom() {
    post("m5/addmember/{uniChatroomGUID}") {
      val config: UniChatroomAddMember = Json.decodeFromString(call.receive())
      ServerController.addMemberToUniChatroom(call, config)
    }
  }

  private fun Route.getMembersOfUniChatroom() {
    get("m5/getmembers/{uniChatroomGUID}") {
      ServerController.getMembersOfUniChatroom(call)
    }
  }

  private fun Route.addRoleToMemberOfUniChatroom() {
    post("m5/addrole/{uniChatroomGUID}") {
      val config: UniChatroomMemberRole = Json.decodeFromString(call.receive())
      ServerController.addRoleToMemberOfUniChatroom(call, config)
    }
  }

  private fun Route.removeRoleOfMemberOfUniChatroom() {
    post("m5/removerole/{uniChatroomGUID}") {
      val config: UniChatroomMemberRole = Json.decodeFromString(call.receive())
      ServerController.removeRoleOfMemberOfUniChatroom(call, config)
    }
  }

  private fun Route.setFirebaseCloudMessagingSubscription() {
    post("m5/subscribe/{uniChatroomGUID}") {
      val config: FirebaseCloudMessagingSubscription = Json.decodeFromString(call.receive())
      ServerController.setFirebaseCloudMessagingSubscription(call, config)
    }
  }
}
