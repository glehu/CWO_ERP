package api.logic.core

import api.logic.webapps.Mockingbird
import api.misc.json.EMailJson
import api.misc.json.FirebaseCloudMessagingSubscription
import api.misc.json.KnowledgeCategoryEdit
import api.misc.json.KnowledgeCreation
import api.misc.json.ListDeltaJson
import api.misc.json.OnlineStateConfig
import api.misc.json.PairLongJson
import api.misc.json.PasswordChange
import api.misc.json.ProcessEntryConfig
import api.misc.json.ProcessInteractionPayload
import api.misc.json.PubKeyPEMContainer
import api.misc.json.SnippetPayload
import api.misc.json.TwoLongOneDoubleJson
import api.misc.json.UniChatroomAddMember
import api.misc.json.UniChatroomAddMessage
import api.misc.json.UniChatroomCreateChatroom
import api.misc.json.UniChatroomImage
import api.misc.json.UniChatroomMemberRole
import api.misc.json.UniChatroomRemoveMember
import api.misc.json.UniMemberProfileImage
import api.misc.json.UniMessageReaction
import api.misc.json.UsernameChange
import api.misc.json.WisdomAnswerCreation
import api.misc.json.WisdomCollaboratorEditPayload
import api.misc.json.WisdomCommentCreation
import api.misc.json.WisdomLessonCreation
import api.misc.json.WisdomQuestionCreation
import api.misc.json.WisdomSearchQuery
import com.github.ajalt.mordant.rendering.TextColors
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
import io.ktor.server.plugins.autohead.*
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
import modules.m2.logic.ContactController
import modules.m4.logic.ItemPriceManager
import modules.m4storage.logic.ItemStorageManager
import modules.m5.logic.UniChatroomController
import modules.m5.logic.getBadges
import modules.m5.logic.giveMessagesBadges
import modules.m5.logic.handleUpgradeUniChatroomRequest
import modules.m6.logic.SnippetBaseController
import modules.m7knowledge.logic.KnowledgeController
import modules.m7wisdom.logic.WisdomController
import modules.m8notification.logic.NotificationController
import modules.m9process.logic.ProcessController
import modules.mx.Ini
import modules.mx.contactIndexManager
import modules.mx.dataPath
import modules.mx.getIniFile
import modules.mx.logic.Log
import modules.mx.logic.UserCLIManager
import modules.mx.logic.exitMain
import modules.mx.programPath
import modules.mx.serverJobGlobal
import modules.mx.terminal
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
    if (System.getenv(iniVal.envPrivKeyPassword) != null) {
      sslConnector(keyStore = keystore, keyAlias = System.getenv(iniVal.envKeyAlias) ?: "?",
                   keyStorePassword = { (System.getenv(iniVal.envKeyStorePassword) ?: "?").toCharArray() },
                   privateKeyPassword = { (System.getenv(iniVal.envPrivKeyPassword) ?: "?").toCharArray() }) {
        host = iniVal.serverIPAddress.substringBefore(':')
        port = 443
      }
    }
    module {
      module()
    }
  }

  val serverEngine = embeddedServer(Netty, environment)

  fun Application.module() {/*
     * #### Plugins ####
     */
    if (System.getenv(iniVal.envPrivKeyPassword) != null) {
      install(HttpsRedirect) {
        sslPort = 443
        permanentRedirect = true
      }
    }
    if (System.getenv("CWOERPSHUTDOWNURL") != null) {
      terminal.println(
              "${TextColors.gray("SERVER")} Initializing Shutdown URL (${System.getenv("CWOERPSHUTDOWNURL")})")
      install(ShutDownUrl.ApplicationCallPlugin) {
        shutDownUrl = System.getenv("CWOERPSHUTDOWNURL")
        exitCodeSupplier = { exitMain() }
      }
    } else {
      terminal.println("${TextColors.gray("SERVER ")} No Shutdown URL")
    }
    install(ContentNegotiation) {
      json(Json {
        prettyPrint = true
        isLenient = true
        encodeDefaults = true
        ignoreUnknownKeys = true
      })
    }
    install(Authentication) {
      basic("auth-basic") {
        realm = "Access to the '/' path"
        validate { credentials ->
          authMutex.withLock {
            if (UserCLIManager.login(
                      email = credentials.name, password = credentials.password, doLog = false)) {
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
                      credential.payload.getClaim("username").asString(), "M*", true)) {
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
    install(AutoHeadResponse)
    // SERVER
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

      getUsercount()

      /*
       * Clarifier WebSocket Session
      */
      webSocket("/clarifier/{unichatroomGUID}") {
        with(UniChatroomController()) {
          this@webSocket.startSession(call)
        }
      }

      /*
       * Clarifier WebSocket Session
      */
      webSocket("/connect") {
        with(Connector) {
          this@webSocket.connect()
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
          // General Endpoints
          sendEMail()
          // M2 Endpoints (Contacts)
          changeUsername()
          changePassword()
          sendFriendRequest()
          getOnlineState()
          // M3 Endpoints (Invoice)
          getOwnInvoices()
          // M4 Endpoints (Item)
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
          // M5 Endpoints (UniChatroom)
          // Creation and Modification
          createUniChatroom()
          getUniChatroom()
          setImageOfUniChatroom()
          setImageOfUniMember()
          setBannerOfUniMember()
          getDirectChatrooms()
          // Messages
          addMessageToUniChatroom()
          getMessagesOfUniChatroom()
          // Members
          addMemberToUniChatroom()
          kickMemberOfUniChatroom()
          banMemberOfUniChatroom()
          getMembersOfUniChatroom()
          getActiveMembersOfUniChatroom()
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
          // M6 Endpoints (SnippetBase)
          saveSnippetImage()
          deleteSnippetImage()
          retrieveSnippetsOfUniChatroom()
          retrieveSnippetsOfWisdom()
          retrieveSnippetsOfProcess()
          // M7 Endpoints (Knowledge)
          getKnowledge()
          createKnowledge()
          configureKnowledgeCategories()
          // Wisdom Creation
          createWisdomQuestion()
          createWisdomAnswer()
          createWisdomComment()
          createWisdomLesson()
          // Wisdom Query
          searchWisdom()
          getWisdomReferences()
          getTopWisdomContributors()
          getWisdom()
          getRecentKeywords()
          getRecentCategories()
          // Wisdom Modification
          reactToWisdom()
          deleteWisdom()
          finishWisdom()
          modifyWisdomContributor()
          // Tasks
          getTasks()
          // Web Solution Endpoints
          addWebshopOrder()
          userTracking()
          // Notification
          getNotifications()
          dismissAllNotifications()
          dismissNotification()
          // Processes
          createProcessEntry()
          getProcesses()
          getProcessEvents()
          getProcessPath()
          interactProcessEvent()
          deleteProcessEvent()
        }
      }
    }
  }

  init {
    if (System.getenv(iniVal.envPrivKeyPassword) != null) {
      // Get HTTPS Certificate
      keystore.load(
              FileInputStream(Paths.get(programPath, "keystore.jks").toString()),
              (System.getenv(iniVal.envCertPassword) ?: "?").toCharArray())
    }
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
              type = Log.Type.COM, text = "User ${call.principal<UserIdPrincipal>()?.name} logout",
              apiEndpoint = call.request.uri)
    }
  }

  private fun Route.login() {
    get("/login") {
      loginMutex.withLock {
        val email = call.principal<UserIdPrincipal>()?.name
        var user: Contact? = null
        contactIndexManager!!.getEntriesFromIndexSearch(
                searchText = "^$email$", ixNr = 1, showAll = true) { user = it as Contact }
        if (user == null) {
          call.respond(HttpStatusCode.NotFound)
        } else {
          call.respond(
                  ServerController.generateLoginResponse(user!!))
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

  private fun Route.addWebshopOrder() {
    post("m3/neworder") {
      if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), "M3")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.placeWebshopOrder(call))
      }
    }
  }

  private fun Route.getPriceCategories() {
    get("m4/pricecategories") {
      if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(
                ItemPriceManager().getCategories())
      }
    }
  }

  private fun Route.getPriceCategoryNumber() {
    get("m4/categorynumber") {
      if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(
                ItemPriceManager().getNumber(ItemPriceManager().getCategories()))
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
                ItemStorageManager().getStorages())
      }
    }
  }

  private fun Route.getStorageNumber() {
    get("m4/storagenumber") {
      if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(
                ItemStorageManager().getNumber(ItemStorageManager().getStorages()))
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
        call.respond(ServerController.checkStorage(call.receive() as TwoLongOneDoubleJson))
      }
    }
  }

  private fun Route.getAvailableStock() {
    post("m4sp/avail") {
      if (!UserCLIManager.checkModuleRight(ServerController.getJWTEmail(call), "M4")) {
        call.respond(HttpStatusCode.Forbidden)
      } else {
        call.respond(ServerController.getAvailableStock(call.receive() as PairLongJson))
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
      ServerController.addMessageToUniChatroom(
              appCall = call, config = config,
              username = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(call))!!.username)
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

  private fun Route.setBannerOfUniMember() {
    post("m5/setmemberbanner/{uniChatroomGUID}") {
      val config: UniMemberProfileImage = Json.decodeFromString(call.receive())
      ServerController.setUniMemberImage(call, config, true)
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

  private fun Route.retrieveSnippetsOfUniChatroom() {
    get("m6/clarifier/{uniChatroomGUID}") {
      val uniChatroomGUID = call.parameters["uniChatroomGUID"]
      if (uniChatroomGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      } else {
        SnippetBaseController().httpGetSnippetsOfUniChatroom(call, uniChatroomGUID)
      }
    }
  }

  private fun Route.retrieveSnippetsOfWisdom() {
    get("m6/wisdom/{wisdomGUID}") {
      val wisdomGUID = call.parameters["wisdomGUID"]
      if (wisdomGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      } else {
        SnippetBaseController().httpGetSnippetsOfWisdom(call, wisdomGUID)
      }
    }
  }

  private fun Route.retrieveSnippetsOfProcess() {
    get("m6/process/{processGUID}") {
      val processGUID = call.parameters["processGUID"]
      if (processGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      } else {
        SnippetBaseController().httpGetSnippetsOfProcess(call, processGUID)
      }
    }
  }

  private fun Route.getSnippetImage() {
    get("m6/get/{snippetGUID}") {
      ServerController.getSnippetResource(call)
    }
  }

  private fun Route.deleteSnippetImage() {
    get("m6/del/{snippetGUID}") {
      ServerController.getSnippetResource(call, true)
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

  private fun Route.getUsercount() {
    get("m2/count") {
      ServerController.getUsercount(call)
    }
  }

  private fun Route.getKnowledge() {
    get("m7/get") {
      val source: String = call.request.queryParameters["src"] ?: ""
      if (source.isEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      val sourceType: String = call.request.queryParameters["from"] ?: ""
      if (sourceType.isEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      if (source.isNotEmpty() && sourceType.isNotEmpty()) {
        if (sourceType == "clarifier") {
          KnowledgeController().httpGetKnowledgeFromUniChatroomGUID(call, source)
        } else if (sourceType == "guid") {
          KnowledgeController().httpGetKnowledgeFromGUID(call, source)
        }
      }
    }
  }

  private fun Route.createKnowledge() {
    post("m7/create") {
      val config: KnowledgeCreation = Json.decodeFromString(call.receive())
      KnowledgeController().httpCreateKnowledge(call, config)
    }
  }

  private fun Route.configureKnowledgeCategories() {
    post("m7/edit/categories/{guid}") {
      val knowledgeGUID = call.parameters["guid"]
      if (knowledgeGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      val config: KnowledgeCategoryEdit = Json.decodeFromString(call.receive())
      if (config.action.isEmpty() || config.category.isEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      KnowledgeController().httpEditKnowledgeCategories(call, config, knowledgeGUID)
    }
  }

  private fun Route.createWisdomQuestion() {
    post("m7/ask") {
      val config: WisdomQuestionCreation = Json.decodeFromString(call.receive())
      val wisdomGUID: String = call.request.queryParameters["guid"] ?: ""
      WisdomController().httpCreateQuestion(call, config, wisdomGUID)
    }
  }

  private fun Route.createWisdomAnswer() {
    post("m7/answer") {
      val config: WisdomAnswerCreation = Json.decodeFromString(call.receive())
      val wisdomGUID: String = call.request.queryParameters["guid"] ?: ""
      WisdomController().httpCreateAnswer(call, config, wisdomGUID)
    }
  }

  private fun Route.createWisdomLesson() {
    post("m7/teach") {
      val config: WisdomLessonCreation = Json.decodeFromString(call.receive())
      val wisdomGUID: String = call.request.queryParameters["guid"] ?: ""
      val mode: String = call.request.queryParameters["mode"] ?: ""
      WisdomController().httpCreateLesson(call, config, wisdomGUID, mode)
    }
  }

  private fun Route.createWisdomComment() {
    post("m7/reply") {
      val config: WisdomCommentCreation = Json.decodeFromString(call.receive())
      val wisdomGUID: String = call.request.queryParameters["guid"] ?: ""
      WisdomController().httpCreateComment(call, config, wisdomGUID)
    }
  }

  private fun Route.searchWisdom() {
    post("m7/search/{knowledgeGUID}") {
      val knowledgeGUID = call.parameters["knowledgeGUID"]
      if (knowledgeGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      val config: WisdomSearchQuery = Json.decodeFromString(call.receive())
      WisdomController().httpWisdomQuery(call, config, knowledgeGUID)
    }
  }

  private fun Route.reactToWisdom() {
    post("m7/react/{wisdomGUID}") {
      val wisdomGUID = call.parameters["wisdomGUID"]
      if (wisdomGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      val config: UniMessageReaction = Json.decodeFromString(call.receive())
      WisdomController().httpWisdomReact(call, config, wisdomGUID)
    }
  }

  private fun Route.getWisdomReferences() {
    get("m7/investigate/{wisdomGUID}") {
      val wisdomGUID = call.parameters["wisdomGUID"]
      if (wisdomGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      val type: String = call.request.queryParameters["type"] ?: "guid"
      WisdomController().httpGetWisdomEntriesRelated(call, wisdomGUID, type)
    }
  }

  private fun Route.getWisdom() {
    get("m7/learn/{wisdomGUID}") {
      val wisdomGUID = call.parameters["wisdomGUID"]
      if (wisdomGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      WisdomController().httpGetWisdomEntry(call, wisdomGUID!!)
    }
  }

  private fun Route.getTopWisdomContributors() {
    get("m7/topwriters/{knowledgeGUID}") {
      val knowledgeGUID = call.parameters["knowledgeGUID"]
      if (knowledgeGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      WisdomController().httpWisdomTopContributors(call, knowledgeGUID)
    }
  }

  private fun Route.deleteWisdom() {
    get("m7/delete/{wisdomGUID}") {
      val wisdomGUID = call.parameters["wisdomGUID"]
      if (wisdomGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      WisdomController().httpDeleteWisdom(call, wisdomGUID)
    }
  }

  private fun Route.finishWisdom() {
    get("m7/finish/{wisdomGUID}") {
      val wisdomGUID = call.parameters["wisdomGUID"]
      if (wisdomGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      val answerGUID: String? = call.request.queryParameters["answer"]
      WisdomController().httpFinishWisdom(call, wisdomGUID, answerGUID)
    }
  }

  private fun Route.getTasks() {
    get("m7/tasks/{knowledgeGUID}") {
      val knowledgeGUID = call.parameters["knowledgeGUID"]
      if (knowledgeGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      val stateFilter: String = call.request.queryParameters["state"] ?: "unfinished"
      WisdomController().httpGetTasks(call, knowledgeGUID, stateFilter)
    }
  }

  private fun Route.modifyWisdomContributor() {
    post("m7/modifycollab/{wisdomGUID}") {
      val wisdomGUID = call.parameters["wisdomGUID"]
      if (wisdomGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      val config: WisdomCollaboratorEditPayload = Json.decodeFromString(call.receive())
      WisdomController().httpModifyWisdomContributor(call, wisdomGUID, config)
    }
  }

  private fun Route.getActiveMembersOfUniChatroom() {
    get("m5/activemembers/{uniChatroomGUID}") {
      val uniChatroomGUID = call.parameters["uniChatroomGUID"]
      if (uniChatroomGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      ServerController.getActiveMembersOfUniChatroom(call, uniChatroomGUID!!)
    }
  }

  private fun Route.getRecentKeywords() {
    get("m7/keywordlist/{knowledgeGUID}") {
      val knowledgeGUID = call.parameters["knowledgeGUID"]
      if (knowledgeGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      WisdomController().httpGetRecentKeywords(call, knowledgeGUID)
    }
  }

  private fun Route.getRecentCategories() {
    get("m7/categorylist/{knowledgeGUID}") {
      val knowledgeGUID = call.parameters["knowledgeGUID"]
      if (knowledgeGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      WisdomController().httpGetRecentCategories(call, knowledgeGUID)
    }
  }

  private fun Route.getDirectChatrooms() {
    get("m5/direct/{username}") {
      val username = call.parameters["username"]
      if (username.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      val getAllQuery: String = call.request.queryParameters["all"] ?: "false"
      var hasToBeJoined = true
      if (getAllQuery == "true") hasToBeJoined = false
      ServerController.getDirectChatrooms(call, username, hasToBeJoined)
    }
  }

  private fun Route.sendFriendRequest() {
    get("m2/befriend/{username}") {
      val username = call.parameters["username"]
      if (username.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      ContactController().httpSendFriendRequest(call, username!!)
    }
  }

  private fun Route.getOnlineState() {
    post("m2/online") {
      val config: OnlineStateConfig = Json.decodeFromString(call.receive())
      ContactController().httpCheckOnlineState(call, config)
    }
  }

  private fun Route.getNotifications() {
    get("m8/notifications") {
      NotificationController().httpGetNotifications(call)
    }
  }

  private fun Route.dismissAllNotifications() {
    get("m8/notifications/dismiss") {
      NotificationController().httpDismissAllNotifications(call)
    }
  }

  private fun Route.dismissNotification() {
    get("m8/notifications/dismiss/{guid}") {
      val notificationGUID = call.parameters["guid"]
      if (notificationGUID.isNullOrEmpty()) {
        NotificationController().httpDismissAllNotifications(call)
      } else {
        NotificationController().httpDismissNotification(call, notificationGUID)
      }
    }
  }

  private fun Route.createProcessEntry() {
    post("m9/create") {
      val config: ProcessEntryConfig = Json.decodeFromString(call.receive())
      val processGUID: String = call.request.queryParameters["guid"] ?: ""
      val mode: String = call.request.queryParameters["mode"] ?: ""
      ProcessController().httpCreateProcessEvent(call, config, processGUID, mode)
    }
  }

  private fun Route.getProcesses() {
    get("m9/processes/{knowledgeGUID}") {
      val knowledgeGUID = call.parameters["knowledgeGUID"]
      if (knowledgeGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      val modeFilter: String = call.request.queryParameters["mode"] ?: "start"
      val authorFilter: String = call.request.queryParameters["author"] ?: ""
      val queryFilter: String = call.request.queryParameters["query"] ?: ""
      ProcessController().httpGetProcesses(call, knowledgeGUID!!, modeFilter, authorFilter, queryFilter)
    }
  }

  private fun Route.getProcessEvents() {
    get("m9/investigate/{knowledgeGUID}") {
      val knowledgeGUID = call.parameters["knowledgeGUID"]
      if (knowledgeGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      val entryPointGUID: String = call.request.queryParameters["entry"] ?: ""
      if (entryPointGUID.isEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      ProcessController().httpGetEventsOfProcess(call, knowledgeGUID!!, entryPointGUID)
    }
  }

  private fun Route.getProcessPath() {
    get("m9/path/{knowledgeGUID}") {
      val knowledgeGUID = call.parameters["knowledgeGUID"]
      if (knowledgeGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      val entryPointGUID: String = call.request.queryParameters["entry"] ?: ""
      if (entryPointGUID.isEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      ProcessController().httpGetFullProcessPath(call, knowledgeGUID!!, entryPointGUID)
    }
  }

  private fun Route.deleteProcessEvent() {
    get("m9/delete/{processGUID}") {
      val processEventGUID = call.parameters["processGUID"]
      if (processEventGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      ProcessController().httpDeleteProcessEvent(call, processEventGUID)
    }
  }

  private fun Route.interactProcessEvent() {
    post("m9/interact/{processGUID}") {
      val config: ProcessInteractionPayload = Json.decodeFromString(call.receive())
      val processEventGUID = call.parameters["processGUID"]
      if (processEventGUID.isNullOrEmpty()) {
        call.respond(HttpStatusCode.BadRequest)
      }
      ProcessController().httpInteractProcessEvent(call, processEventGUID, config)
    }
  }
}
