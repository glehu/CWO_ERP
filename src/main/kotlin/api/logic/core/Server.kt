package api.logic.core

import api.logic.webapps.Mockingbird
import api.logic.webapps.WebPlanner
import api.misc.json.EMailJson
import api.misc.json.EntryJson
import api.misc.json.FirebaseCloudMessagingSubscription
import api.misc.json.ListDeltaJson
import api.misc.json.PairIntJson
import api.misc.json.PasswordChange
import api.misc.json.PubKeyPEMContainer
import api.misc.json.SettingsRequestJson
import api.misc.json.SnippetPayload
import api.misc.json.TwoIntOneDoubleJson
import api.misc.json.UniChatroomAddMember
import api.misc.json.UniChatroomAddMessage
import api.misc.json.UniChatroomCreateChatroom
import api.misc.json.UniChatroomImage
import api.misc.json.UniChatroomMemberRole
import api.misc.json.UniChatroomRemoveMember
import api.misc.json.UniMemberProfileImage
import api.misc.json.UsernameChange
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.plugins.httpsredirect.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.m2.Contact
import modules.m4.logic.ItemPriceManager
import modules.m4storage.logic.ItemStorageManager
import modules.m5.logic.UniChatroomController
import modules.m5.logic.getBadges
import modules.m5.logic.giveMessagesBadges
import modules.m5.logic.handleUpgradeUniChatroomRequest
import modules.mx.Ini
import modules.mx.contactIndexManager
import modules.mx.dataPath
import modules.mx.discographyIndexManager
import modules.mx.getIniFile
import modules.mx.invoiceIndexManager
import modules.mx.itemIndexManager
import modules.mx.itemStockPostingIndexManager
import modules.mx.logic.Log
import modules.mx.logic.UserCLIManager
import modules.mx.programPath
import modules.mx.serverJobGlobal
import modules.mx.uniChatroomIndexManager
import modules.mx.usageTracker
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.nio.file.Paths
import java.security.KeyStore
import java.time.Duration
import kotlin.text.toCharArray

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

  private val authMutex = Mutex()
  private val loginMutex = Mutex()

  private val iniVal = Json.decodeFromString<Ini>(getIniFile().readText())
  lateinit var text: String
  private val keystore: KeyStore = KeyStore.getInstance("JKS")
  private val environment = applicationEngineEnvironment {
    log = LoggerFactory.getLogger("ktor.application")
    connector {
      host = iniVal.serverIPAddress.substringBefore(':')
      port = 80
    }
    sslConnector(keyStore = keystore,
      keyAlias = System.getenv(iniVal.envKeyAlias) ?: "?",
      keyStorePassword = { (System.getenv(iniVal.envKeyStorePassword) ?: "?").toCharArray() },
      privateKeyPassword = { (System.getenv(iniVal.envPrivKeyPassword) ?: "?").toCharArray() }) {
      host = iniVal.serverIPAddress.substringBefore(':')
      port = 443
    }
    module {
      module()
    }
  }

  val serverEngine = embeddedServer(Netty, environment)

  fun Application.module() {
    /*
     * #### Plugins ####
     */
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
          authMutex.withLock {
            if (UserCLIManager.login(
                email = credentials.name, password = credentials.password, doLog = false
              )) {
              UserIdPrincipal(credentials.name)
            } else {
              null
            }
          }
        }
      }
      jwt("auth-jwt") {
        realm = "Access to the '/' path"
        verifier(ServerController.buildJWTVerifier(iniVal))
        // Check if user can access any module
        validate { credential ->
          authMutex.withLock {
            if (UserCLIManager.checkModuleRight(
                credential.payload.getClaim("username").asString(), "M*"
              )) {
              JWTPrincipal(credential.payload)
            } else {
              null
            }
          }
        }
      }
    }
    install(CORS) {
      anyHost()
      allowHeader(HttpHeaders.ContentType)
      allowHeader(HttpHeaders.Authorization)
    }
    install(WebSockets) {
      pingPeriod = Duration.ofSeconds(5)
      timeout = Duration.ofSeconds(20)
      maxFrameSize = Long.MAX_VALUE
      masking = false
    }
    install(DoubleReceive)
    /*
     * #### Routing ####
     */
    routing {
      route("/") {
        get {
          call.respondRedirect("https://wikiric.netlify.app/")
        }
      }
      route("/status") {
        get {
          call.respondFile(File(Paths.get(dataPath, "data", "web", "home.html").toString()))
        }
      }
      route("/mockingbird") {
        post {
          Mockingbird.handleRequest(call)
        }
      }
      register()
      spotifyAuthCallback()

      // SnippetBase
      getSnippetImage()

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
        route("/api") {
          /*
           * General Endpoints
           */
          getIndexSelection(
            discographyIndexManager!!,
            contactIndexManager!!,
            invoiceIndexManager!!,
            itemIndexManager!!,
            itemStockPostingIndexManager!!,
            uniChatroomIndexManager!!
          )
          getEntry(
            discographyIndexManager!!,
            contactIndexManager!!,
            invoiceIndexManager!!,
            itemIndexManager!!,
            itemStockPostingIndexManager!!,
            uniChatroomIndexManager!!
          )
          saveEntry(
            discographyIndexManager!!,
            contactIndexManager!!,
            invoiceIndexManager!!,
            itemIndexManager!!,
            itemStockPostingIndexManager!!,
            uniChatroomIndexManager!!
          )
          getEntryLock(
            discographyIndexManager!!,
            contactIndexManager!!,
            invoiceIndexManager!!,
            itemIndexManager!!,
            itemStockPostingIndexManager!!,
            uniChatroomIndexManager!!
          )
          setEntryLock(
            discographyIndexManager!!,
            contactIndexManager!!,
            invoiceIndexManager!!,
            itemIndexManager!!,
            itemStockPostingIndexManager!!,
            uniChatroomIndexManager!!
          )
          sendEMail()
          getSettingsFileText()

          /*
           * M2 Endpoints (Contacts)
           */
          changeUsername()
          changePassword()

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
          // Creation and Modification
          createUniChatroom()
          getUniChatroom()
          setImageOfUniChatroom()
          setImageOfUniMember()
          // Messages
          addMessageToUniChatroom()
          getMessagesOfUniChatroom()
          // Members
          addMemberToUniChatroom()
          kickMemberOfUniChatroom()
          banMemberOfUniChatroom()
          getMembersOfUniChatroom()
          // Roles
          addRoleToMemberOfUniChatroom()
          removeRoleOfMemberOfUniChatroom()
          // Notifications
          setFirebaseCloudMessagingSubscription()
          // Encryption
          setPubKeyPEM()
          // Reward System
          upgradeUniChatroom()
          giveMessageBadges()
          getBadges()

          /*
           * M6 Endpoints (SnippetBase)
           */
          saveSnippetImage()

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
    val options = FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault()).build()
    FirebaseApp.initializeApp(options)
  }

  private fun Route.logout() {
    get("/logout") {
      log(
        type = Log.Type.COM,
        text = "User ${call.principal<UserIdPrincipal>()?.name} logout",
        apiEndpoint = call.request.uri
      )
    }
  }

  private fun Route.login() {
    get("/login") {
      loginMutex.withLock {
        val email = call.principal<UserIdPrincipal>()?.name
        var user: Contact? = null
        contactIndexManager!!.getEntriesFromIndexSearch(
          searchText = "^$email$", ixNr = 1, showAll = true
        ) { user = it as Contact }
        if (user == null) {
          call.respond(HttpStatusCode.NotFound)
        } else {
          call.respond(
            ServerController.generateLoginResponse(user!!)
          )
        }
      }
    }
  }

  private fun Route.spotifyAuthCallback() {
    get("/authcallback/spotify") {
      val code: String? = call.request.queryParameters["code"]
      if (code != null) {
        call.respondFile(File(Paths.get(dataPath, "data", "web", "spotifyCallback.html").toString()))
        log(Log.Type.COM, "Spotify Auth Callback received")
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
        if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), ix.module)) {
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
        if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), ix.module)) {
          call.respond(HttpStatusCode.Forbidden)
        } else {
          call.respond(
            ServerController.getEntry(
              appCall = call, indexManager = ix
            )
          )
        }
      }
    }
  }

  private fun Route.saveEntry(vararg indexManager: IIndexManager) {
    for (ix in indexManager) {
      post("${ix.module.lowercase()}/saveentry") {
        if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), ix.module)) {
          call.respond(HttpStatusCode.Forbidden)
        } else {
          val entryJson: EntryJson = call.receive()
          call.respond(
            ServerController.saveEntry(
              entry = entryJson.entry, indexManager = ix, username = ServerController.getJWTEmail(call)
            )
          )
        }
      }
    }
  }

  private fun Route.getEntryLock(vararg indexManager: IIndexManager) {
    for (ix in indexManager) {
      get("${ix.module.lowercase()}/getentrylock/{searchString}") {
        if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), ix.module)) {
          call.respond(HttpStatusCode.Forbidden)
        } else {
          call.respond(
            ServerController.getEntryLock(
              appCall = call, indexManager = ix
            )
          )
        }
      }
    }
  }

  private fun Route.setEntryLock(vararg indexManager: IIndexManager) {
    for (ix in indexManager) {
      get("${ix.module.lowercase()}/setentrylock/{searchString}") {
        if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), ix.module)) {
          call.respond(HttpStatusCode.Forbidden)
        } else {
          call.respond(
            ServerController.setEntryLock(
              appCall = call, indexManager = ix
            )
          )
        }
      }
    }
  }

  private fun Route.addWebshopOrder() {
    post("m3/neworder") {
      if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), "M3")) {
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
      if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), "M4")) {
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
      if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), "M4")) {
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
      if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.updatePriceCategories(call.receive() as ListDeltaJson))
      }
    }
  }

  private fun Route.deletePriceCategory() {
    post("m4/deletecategory") {
      if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.deletePriceCategory(call.receive() as ListDeltaJson))
      }
    }
  }

  private fun Route.getStorages() {
    get("m4/storages") {
      if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), "M4")) {
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
      if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), "M4")) {
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
      if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.updateStorages(call.receive() as ListDeltaJson))
      }
    }
  }

  private fun Route.deleteStorage() {
    post("m4/deletestorage") {
      if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.deleteStorage(call.receive() as ListDeltaJson))
      }
    }
  }

  private fun Route.getItemImage() {
    get("m4/getimage/{itemUID}") {
      if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.getItemImage())
      }
    }
  }

  private fun Route.checkStorage() {
    post("m4sp/check") {
      if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.checkStorage(call.receive() as TwoIntOneDoubleJson))
      }
    }
  }

  private fun Route.getAvailableStock() {
    post("m4sp/avail") {
      if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), "M4")) {
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
      if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), body.module)) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(
          ServerController.getSettingsFileText(
            moduleShort = body.module, subSetting = body.subSetting
          )
        )
      }
    }
  }

  private fun Route.createUniChatroom() {
    post("m5/createchatroom") {
      val config: UniChatroomCreateChatroom = Json.decodeFromString(call.receive())
      val user = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(call))
      if (user == null) {
        call.respond(HttpStatusCode.Unauthorized)
      } else {
        ServerController.createUniChatroom(call, config, user.username)
      }
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
      ServerController.addMessageToUniChatroom(call, config, ServerController.getJWTEmail(call))
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

  private fun Route.kickMemberOfUniChatroom() {
    post("m5/kickmember/{uniChatroomGUID}") {
      val config: UniChatroomRemoveMember = Json.decodeFromString(call.receive())
      ServerController.removeMemberOfUniChatroom(call, config)
    }
  }

  private fun Route.banMemberOfUniChatroom() {
    post("m5/banmember/{uniChatroomGUID}") {
      val config: UniChatroomRemoveMember = Json.decodeFromString(call.receive())
      ServerController.banMemberOfUniChatroom(call, config)
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
      if (config.role.uppercase() == "OWNER") {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        ServerController.addRoleToMemberOfUniChatroom(call, config)
      }
    }
  }

  private fun Route.removeRoleOfMemberOfUniChatroom() {
    post("m5/removerole/{uniChatroomGUID}") {
      val config: UniChatroomMemberRole = Json.decodeFromString(call.receive())
      if (config.role.uppercase() == "OWNER") {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        ServerController.removeRoleOfMemberOfUniChatroom(call, config)
      }
    }
  }

  private fun Route.setFirebaseCloudMessagingSubscription() {
    post("m5/subscribe/{uniChatroomGUID}") {
      val config: FirebaseCloudMessagingSubscription = Json.decodeFromString(call.receive())
      ServerController.setFirebaseCloudMessagingSubscription(call, config)
    }
  }

  private fun Route.setPubKeyPEM() {
    post("m5/pubkey/{uniChatroomGUID}") {
      val config: PubKeyPEMContainer = Json.decodeFromString(call.receive())
      ServerController.setPubKeyPEM(call, config)
    }
  }

  private fun Route.setImageOfUniChatroom() {
    post("m5/setimage/{uniChatroomGUID}") {
      val config: UniChatroomImage = Json.decodeFromString(call.receive())
      ServerController.setUniChatroomImage(call, config)
    }
  }

  private fun Route.setImageOfUniMember() {
    post("m5/setmemberimage/{uniChatroomGUID}") {
      val config: UniMemberProfileImage = Json.decodeFromString(call.receive())
      ServerController.setUniMemberImage(call, config)
    }
  }

  private fun Route.upgradeUniChatroom() {
    post("m5/upgrade/{uniChatroomGUID}") {
      handleUpgradeUniChatroomRequest(call, Json.decodeFromString(call.receive()))
    }
  }

  private fun Route.giveMessageBadges() {
    get("m2/badges/set/{uniChatroomGUID}") {
      giveMessagesBadges(call.parameters["uniChatroomGUID"], call)
    }
  }

  private fun Route.getBadges() {
    get("m2/badges/get/{username}") {
      getBadges(call.parameters["username"], call)
    }
  }

  private fun Route.saveSnippetImage() {
    post("m6/create") {
      val payload: SnippetPayload = Json.decodeFromString(call.receive())
      ServerController.createSnippetResource(call, payload)
    }
  }

  private fun Route.getSnippetImage() {
    get("m6/get/{snippetGUID}") {
      ServerController.getSnippetResource(call)
    }
  }

  private fun Route.changeUsername() {
    post("m2/edit/username") {
      val payload: UsernameChange = Json.decodeFromString(call.receive())
      ServerController.changeUsername(call, payload)
    }
  }

  private fun Route.changePassword() {
    post("m2/edit/credentials") {
      val payload: PasswordChange = Json.decodeFromString(call.receive())
      ServerController.changePassword(call, payload)
    }
  }
}
