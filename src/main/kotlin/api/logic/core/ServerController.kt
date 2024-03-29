package api.logic.core

import api.misc.json.ActiveMembersPayload
import api.misc.json.FirebaseCloudMessagingSubscription
import api.misc.json.ListDeltaJson
import api.misc.json.LoginResponseJson
import api.misc.json.PairLongJson
import api.misc.json.PasswordChange
import api.misc.json.PubKeyPEMContainer
import api.misc.json.RegistrationPayload
import api.misc.json.RegistrationResponse
import api.misc.json.SnippetPayload
import api.misc.json.SnippetResponse
import api.misc.json.TwoLongOneDoubleJson
import api.misc.json.UniChatroomAddMember
import api.misc.json.UniChatroomAddMessage
import api.misc.json.UniChatroomCreateChatroom
import api.misc.json.UniChatroomImage
import api.misc.json.UniChatroomMemberRole
import api.misc.json.UniChatroomMessages
import api.misc.json.UniChatroomRemoveMember
import api.misc.json.UniMemberProfileImage
import api.misc.json.UsernameChange
import api.misc.json.ValidationContainerJson
import api.misc.json.WebshopOrder
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.WebpushConfig
import com.google.firebase.messaging.WebpushFcmOptions
import com.google.firebase.messaging.WebpushNotification
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m2.Contact
import modules.m3.Invoice
import modules.m3.InvoicePosition
import modules.m3.logic.InvoiceCLIController
import modules.m4.Item
import modules.m4.ItemPriceCategory
import modules.m4.logic.ItemPriceManager
import modules.m4stockposting.logic.ItemStockPostingController
import modules.m4storage.logic.ItemStorageManager
import modules.m5.UniChatroom
import modules.m5.UniChatroomRole
import modules.m5.UniMember
import modules.m5.UniRole
import modules.m5.logic.UniChatroomController
import modules.m5messages.UniMessage
import modules.m6.logic.SnippetBaseController
import modules.m7knowledge.Knowledge
import modules.m7knowledge.logic.KnowledgeController
import modules.m7wisdom.logic.WisdomController
import modules.m9process.logic.ProcessController
import modules.mx.Ini
import modules.mx.contactIndexManager
import modules.mx.dataPath
import modules.mx.getIniFile
import modules.mx.invoiceIndexManager
import modules.mx.itemIndexManager
import modules.mx.logic.Emailer
import modules.mx.logic.Log
import modules.mx.logic.UserCLIManager
import modules.mx.logic.encryptKeccak
import modules.mx.maxSearchResultsGlobal
import modules.mx.uniMessagesIndexManager
import java.io.File
import java.math.RoundingMode
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.set
import kotlin.math.ceil

@ExperimentalSerializationApi
class ServerController {
  @ExperimentalCoroutinesApi
  @DelicateCoroutinesApi
  @InternalAPI
  @ExperimentalSerializationApi
  companion object Server : IModule {
    override val moduleNameLong = "ServerController"
    override val module = "MX"
    override fun getIndexManager(): IIndexManager? {
      return null
    }

    val iniVal = Json.decodeFromString<Ini>(getIniFile().readText())

    private val mutex = Mutex()

    suspend fun saveEntry(
      entry: ByteArray,
      indexManager: IIndexManager,
      username: String
    ): Long {
      val uID: Long
      mutex.withLock {
        log(
                type = Log.Type.COM, text = "API ${indexManager.module} entry save",
                apiEndpoint = "/api/${indexManager.module}/save", moduleAlt = indexManager.module)
        uID = indexManager.save(
                entry = indexManager.decode(entry), userName = username)
      }
      return uID
    }

    fun getEntry(
      appCall: ApplicationCall,
      indexManager: IIndexManager
    ): Any {
      val routePar = appCall.parameters["searchString"]
      if (!routePar.isNullOrEmpty()) {
        when (appCall.request.queryParameters["type"]) {
          "uid" -> {
            val doLock = when (appCall.request.queryParameters["lock"]) {
              "true" -> true
              else -> false
            }
            return if (appCall.request.queryParameters["format"] == "json") {
              val entry = indexManager.decode(
                      indexManager.getBytes(uID = routePar.toLong()))
              entry.initialize()
              indexManager.encodeToJsonString(
                      entry = entry, prettyPrint = true)
            } else {
              indexManager.getBytes(
                      uID = routePar.toLong(), lock = doLock,
                      userName = appCall.principal<JWTPrincipal>()!!.payload.getClaim("username").asString())
            }
          }

          "name" -> {
            val requestIndex = appCall.request.queryParameters["index"]
            val ixNr: Int = (requestIndex?.toInt()) ?: 1
            val exactSearch = requestIndex != null
            return if (appCall.request.queryParameters["format"] == "json") {
              indexManager.getEntryListJson(
                      searchText = routePar, ixNr, exactSearch = exactSearch)
            } else {
              indexManager.getEntryBytesListJson(
                      searchText = routePar, ixNr, exactSearch = exactSearch)
            }
          }

          else -> return ""
        }
      }
      return ""
    }

    fun getEntryLock(
      appCall: ApplicationCall,
      indexManager: IIndexManager
    ): Boolean {
      val routePar = appCall.parameters["searchString"]
      if (!routePar.isNullOrEmpty()) {
        return indexManager.getEntryLock(
                uID = routePar.toLong(),
                userName = appCall.principal<JWTPrincipal>()!!.payload.getClaim("username").asString())
      }
      return false
    }

    suspend fun setEntryLock(
      appCall: ApplicationCall,
      indexManager: IIndexManager
    ): Boolean {
      val routePar = appCall.parameters["searchString"]
      val queryPar = appCall.request.queryParameters["type"]
      val success: Boolean = if (!routePar.isNullOrEmpty()) {
        mutex.withLock {
          indexManager.setEntryLock(
                  uID = routePar.toLong(), doLock = queryPar.toBoolean(),
                  userName = appCall.principal<JWTPrincipal>()!!.payload.getClaim("username").asString())
        }
      } else false
      return success
    }

    @InternalAPI
    suspend fun generateLoginResponse(user: Contact): ValidationContainerJson {
      mutex.withLock {
        val iniVal = Json.decodeFromString<Ini>(getIniFile().readText())
        //                 h   min  sec  ms
        val expiresInMs = (1 * 60 * 60 * 1000)
        val expiresAt = Date(System.currentTimeMillis() + expiresInMs)
        val token = JWT.create().withAudience("https://${iniVal.serverIPAddress}/")
          .withIssuer("https://${iniVal.serverIPAddress}/").withClaim("username", user.email).withExpiresAt(expiresAt)
          .sign(Algorithm.HMAC256(iniVal.token))
        val loginResponse = Json.encodeToString(
                LoginResponseJson(
                        httpCode = 200, username = user.username, token = token, expiresInMs = expiresInMs,
                        accessM1 = user.canAccessDiscography, accessM2 = user.canAccessContacts,
                        accessM3 = user.canAccessInvoices, accessM4 = user.canAccessInventory,
                        accessM5 = user.canAccessClarifier, accessM6 = user.canAccessSnippetBase))
        return ValidationContainerJson(
                contentJson = loginResponse, hash = encryptKeccak(
                input = loginResponse, salt = encryptKeccak(user.email),
                pepper = encryptKeccak("CWO_ERP LoginValidation")))
      }
    }

    fun buildJWTVerifier(iniVal: Ini): JWTVerifier {
      return JWT.require(Algorithm.HMAC256(iniVal.token)).withAudience("https://${iniVal.serverIPAddress}/")
        .withIssuer("https://${iniVal.serverIPAddress}/").build()
    }

    @InternalAPI
    fun updatePriceCategories(categoryJson: ListDeltaJson): Boolean {
      ItemPriceManager().updateCategory(
              categoryNew = Json.decodeFromString(categoryJson.listEntryNew),
              categoryOld = Json.decodeFromString(categoryJson.listEntryOld))
      return true
    }

    @InternalAPI
    fun deletePriceCategory(categoryJson: ListDeltaJson): Boolean {
      ItemPriceManager().deleteCategory(
              category = Json.decodeFromString(categoryJson.listEntryNew),
      )
      return true
    }

    @InternalAPI
    fun updateStorages(categoryJson: ListDeltaJson): Boolean {
      ItemStorageManager().funUpdateStorage(
              storageNew = Json.decodeFromString(categoryJson.listEntryNew),
              storageOld = Json.decodeFromString(categoryJson.listEntryOld))
      return true
    }

    @InternalAPI
    fun deleteStorage(categoryJson: ListDeltaJson): Boolean {
      ItemStorageManager().deleteStorage(
              storage = Json.decodeFromString(categoryJson.listEntryNew),
      )
      return true
    }

    suspend fun placeWebshopOrder(appCall: ApplicationCall): Long {
      val m3IniVal = InvoiceCLIController().getIni()

      /**
       * The webshop order
       */
      val order = Invoice(-1)

      /**
       * The ordered item's UID
       */
      val body = appCall.receive<WebshopOrder>()
      if (body.cart.isEmpty()) return -1
      for (i in 0 until body.cart.size) {
        val item = itemIndexManager!!.get(body.cart[i].uID) as Item
        if (item.uID != -1L) {
          /**
           * Item position
           */
          val itemPosition = InvoicePosition(item.uID, item.description)
          itemPosition.grossPrice = Json.decodeFromString<ItemPriceCategory>(item.prices[0]!!).grossPrice
          itemPosition.amount = body.cart[i].amount.toDouble()
          order.items[i] = Json.encodeToString(itemPosition)
        }
      }
      order.customerNote = body.customerNote/*
       * Finalize the order and save it
       */
      val userName = appCall.principal<JWTPrincipal>()!!.payload.getClaim("username").asString()
      InvoiceCLIController().calculate(order)
      order.date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
      order.text = "Web Order"
      order.buyer = userName
      order.status = if (m3IniVal.autoCommission) 1 else 0
      order.statusText = InvoiceCLIController().getStatusText(order.status)
      //Check if the customer is an existing contact, if not, create it
      val contactsMatched = contactIndexManager!!.getEntryBytesListJson(
              searchText = userName, ixNr = 1, exactSearch = false)
      if (contactsMatched.resultsList.isEmpty() && m3IniVal.autoCreateContacts) {
        val contact = Contact(-1, userName)
        contact.email = userName
        contact.moneySent = order.netTotal
        mutex.withLock {
          order.buyerUID = contactIndexManager!!.save(contact)
        }
      } else {
        val contact = contactIndexManager!!.decode(contactsMatched.resultsList[0]) as Contact
        order.buyerUID = contact.uID
        if (contact.email.isEmpty() || contact.email == "?") contact.email = userName
        contact.moneySent = order.netTotal
        mutex.withLock {
          contactIndexManager!!.save(contact)
        }
      }
      order.seller = "<Self>"
      if (m3IniVal.autoSendEmailConfirmation) order.emailConfirmationSent = true
      mutex.withLock {
        invoiceIndexManager!!.save(entry = order, userName = userName)
      }
      mutex.withLock {
        log(
                type = Log.Type.COM, text = "web shop order #${order.uID} from ${order.buyer}",
                apiEndpoint = appCall.request.uri, moduleAlt = invoiceIndexManager!!.module)
      }
      if (m3IniVal.autoSendEmailConfirmation) {
        Emailer().sendEmail(
                subject = "Web Shop Order #${order.uID}",
                body = "Hey, we're confirming your order over ${order.grossTotal} Euro.\n" + "Order Number: #${order.uID}\n" + "Date: ${order.date}",
                recipient = order.buyer)
      }
      return order.uID
    }

    suspend fun registerUser(appCall: ApplicationCall): RegistrationResponse {
      val registrationPayload = appCall.receive<RegistrationPayload>()
      var exists = false
      var message = ""
      if (registrationPayload.username == "_server") {
        message = "Entered Username not allowed"
        return RegistrationResponse(false, message)
      }
      mutex.withLock {
        // Check if email is registered already
        contactIndexManager!!.getEntriesFromIndexSearch(
                searchText = "^${registrationPayload.email}$", ixNr = 1, showAll = true) {
          exists = true
        } // Set 'exists' to true if anything was found
        if (exists) {
          message = "User with entered Email already exists"
          return RegistrationResponse(false, message)
        }
        // Check if username is registered already
        contactIndexManager!!.getEntriesFromIndexSearch(
                searchText = "^${registrationPayload.username}$", ixNr = 2, showAll = true) {
          exists = true
        } // Set 'exists' to true if anything was found
        if (exists) {
          message = "User with entered Username already exists"
          return RegistrationResponse(false, message)
        }
        // Create a new user
        val newUser = Contact(-1, registrationPayload.username)
        newUser.email = registrationPayload.email
        newUser.username = registrationPayload.username
        newUser.password = encryptKeccak(registrationPayload.password)
        contactIndexManager!!.save(newUser)
        log(Log.Type.COM, "User ${registrationPayload.username} registered.", appCall.request.uri)
      }
      return RegistrationResponse(true, message)
    }

    fun getItemImage(): String {
      val sampleImg = File(Paths.get(dataPath, "data", "img", "orochi_logo_red_500x500.png").toString())
      return Base64.getEncoder().encodeToString(sampleImg.readBytes())
    }

    fun getJWTEmail(appCall: ApplicationCall): String {
      return appCall.principal<JWTPrincipal>()!!.payload.getClaim("username").asString()
    }

    fun getOwnInvoices(appCall: ApplicationCall): Any {
      return invoiceIndexManager!!.getEntryListJson(
              searchText = getJWTEmail(appCall), ixNr = 2, exactSearch = true)
    }

    fun checkStorage(request: TwoLongOneDoubleJson): Boolean {
      return ItemStockPostingController().check(request.first, request.second, request.third)
    }

    fun getAvailableStock(request: PairLongJson): Double {
      return ItemStockPostingController().getAvailableStock(request.first, request.second)
    }

    /**
     * Introduce delay avoiding having to deal with this load heavy operation too often
     */
    suspend fun pauseRequest(durationMs: Long) {
      log(Log.Type.COM, "Pausing request for $durationMs ms...")
      delay(durationMs)
      log(Log.Type.COM, "Resuming request that waited for $durationMs ms...")
    }

    suspend fun createUniChatroom(
      appCall: ApplicationCall,
      config: UniChatroomCreateChatroom,
      owner: String,
    ) {
      val parentUniChatroomGUID: String = appCall.request.queryParameters["parent"] ?: ""
      val uniChatroom: UniChatroom
      with(UniChatroomController()) {
        // Create Chatroom and populate it
        uniChatroom = createChatroom(config.title, config.type)
        if (config.directMessageUsernames.isEmpty()) {
          uniChatroom.addOrUpdateMember(
                  username = owner, role = UniRole("Owner"))
        } else {
          uniChatroom.directMessageUsername = ""
          var addedDirectMembers = false
          var amount = 0
          for (user in config.directMessageUsernames) {
            amount++
            if (user.isNotEmpty()) {
              addedDirectMembers = true
              uniChatroom.directMessageUsername += "|$user|"
              uniChatroom.addOrUpdateMember(
                      username = user, role = UniRole("Owner"))
            }
            // Only two people can participate in a direct message
            if (amount == 2) break
          }
          if (!addedDirectMembers) {
            appCall.respond(HttpStatusCode.BadRequest)
            return
          }
          uniChatroom.type = "direct"
        }
        // Set Chatroom Image if provided
        if (config.imgBase64.isNotEmpty()) {
          uniChatroom.imgGUID = config.imgBase64
        }
        // Update parent chatroom with this chatroom's GUID if needed
        if (parentUniChatroomGUID.isNotEmpty()) {
          val parent: UniChatroom?
          mutex.withLock {
            parent = getChatroom(parentUniChatroomGUID)
            if (parent == null) {
              appCall.respond(HttpStatusCode.NotFound)
              return
            }
            // We initialize here since we didn't save yet, thus having no GUID to reference etc.
            uniChatroom.uID = -2 // Little bypass
            uniChatroom.initialize()
            uniChatroom.uID = -1
            // Create a copy since we want to remove unnecessary stuff before saving it into the parent
            val copy = uniChatroom.copy()
            copy.imgGUID = ""
            copy.members.clear()
            // Copy other values
            copy.chatroomGUID = uniChatroom.chatroomGUID
            copy.parentGUID = uniChatroom.parentGUID
            copy.type = uniChatroom.type
            // Create reference
            parent.subChatrooms.add(Json.encodeToString(copy))
            // Reference the parent also
            uniChatroom.parentGUID = parent.chatroomGUID
            saveChatroom(parent)
          }
        }
        mutex.withLock {
          saveChatroom(uniChatroom)
        }
        uniChatroom.addMessage(
                member = "_server", message = "[s:RegistrationNotification]${owner} has created ${config.title}!")
      }
      appCall.respond(uniChatroom)
    }

    suspend fun getUniChatroom(appCall: ApplicationCall) {
      val uniChatroomGUID = appCall.parameters["uniChatroomGUID"]
      if (uniChatroomGUID.isNullOrEmpty()) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      val uniChatroom: UniChatroom?
      with(UniChatroomController()) {
        mutex.withLock {
          uniChatroom = getChatroom(uniChatroomGUID)
          if (uniChatroom == null) {
            appCall.respond(HttpStatusCode.NotFound)
          } else {
            if (uniChatroom.checkIsMemberBanned(
                      username = getJWTEmail(appCall), isEmail = true)) {
              appCall.respond(HttpStatusCode.Forbidden)
              return
            }
            appCall.respond(uniChatroom)
          }
        }
      }
    }

    suspend fun addMessageToUniChatroom(
      appCall: ApplicationCall,
      config: UniChatroomAddMessage,
      username: String
    ) {
      with(UniChatroomController()) {
        mutex.withLock {
          val uniChatroom: UniChatroom? = getChatroom(config.uniChatroomGUID)
          if (uniChatroom == null) {
            appCall.respond(HttpStatusCode.NotFound)
            return
          }
          if (uniChatroom.checkIsMemberBanned(
                    username = username, isEmail = false)) {
            appCall.respond(HttpStatusCode.Forbidden)
            return
          }
          // Is this action allowed?
          val userRights = applyUniChatroomRights(uniChatroom, username = username)
          if (!userRights.canWrite) {
            appCall.respond(HttpStatusCode.Forbidden)
            return
          }
          if (!uniChatroom.addMessage(username, config.text)) {
            appCall.respond(HttpStatusCode.BadRequest)
            return
          }
          val session = UniChatroomController.clarifierSessions[config.uniChatroomGUID] ?: return
          val prefix = "[c:MSG<ENCR]"
          var isEncrypted = false
          if (config.text.startsWith(prefix)) isEncrypted = true
          val uniMessage = UniMessage(
                  uniChatroomUID = uniChatroom.uID, from = username, message = config.text)
          uniMessage.isEncrypted = isEncrypted
          val msg = Json.encodeToString(uniMessage)
          session.clarifierSessionMutex.withLock {
            session.clarifierSessionUsers.forEach {
              if (!it.value.session.outgoing.isClosedForSend) {
                it.value.session.send(Frame.Text(msg))
              }
            }
          }
          // Send notification to all members
          val json = Json {
            isLenient = true
            ignoreUnknownKeys = true
          }
          val fcmTokens: ArrayList<String> = arrayListOf()
          for (memberJson in uniChatroom.members) {
            val member: UniMember = json.decodeFromString(memberJson)
            if (member.firebaseCloudMessagingToken.isEmpty()) continue
            fcmTokens.add(member.firebaseCloudMessagingToken)
          }
          if (fcmTokens.isNotEmpty()) {/*
                  Build the notification
                  If there's a parentGUID, then this chatroom must be a subchat
                */
            lateinit var destination: String
            lateinit var subchatGUID: String
            if (uniChatroom.parentGUID.isNotEmpty()) {
              destination = uniChatroom.parentGUID
              subchatGUID = uniChatroom.chatroomGUID
            } else {
              destination = uniChatroom.chatroomGUID
              subchatGUID = ""
            }
            val message = MulticastMessage.builder().setWebpushConfig(
                    WebpushConfig.builder().setNotification(
                            WebpushNotification(
                                    uniChatroom.title, "$username has sent a message.")).setFcmOptions(
                            WebpushFcmOptions.withLink("/apps/clarifier/wss/$destination"))
                      .putData("dlType", "clarifier").putData("dlDest", "/apps/clarifier/wss/$destination")
                      .putData("subchatGUID", subchatGUID).build()).addAllTokens(fcmTokens).build()
            FirebaseMessaging.getInstance().sendMulticast(message)
          }
        }
      }
      appCall.respond(HttpStatusCode.OK)
    }

    suspend fun getMessagesOfUniChatroom(appCall: ApplicationCall) {
      val uniChatroomGUID = appCall.parameters["uniChatroomGUID"]
      if (uniChatroomGUID.isNullOrEmpty()) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      val pageIndex: Int = appCall.request.queryParameters["pageIndex"]?.toInt() ?: 0
      val pageSize: Int = appCall.request.queryParameters["pageSize"]?.toInt() ?: maxSearchResultsGlobal
      val skipCount: Int = appCall.request.queryParameters["skip"]?.toInt() ?: 0
      with(UniChatroomController()) {
        val uniChatroom = getChatroom(uniChatroomGUID)
        if (uniChatroom == null) {
          appCall.respond(HttpStatusCode.NotFound)
          return
        } else {
          val username = UserCLIManager.getUserFromEmail(getJWTEmail(appCall))!!.username
          if (uniChatroom.checkIsMemberBanned(
                    username = username, isEmail = false)) {
            appCall.respond(HttpStatusCode.Forbidden)
            return
          }
          // Is this action allowed?
          val userRights = applyUniChatroomRights(uniChatroom, username = username)
          if (!userRights.canRead) {
            appCall.respond(HttpStatusCode.Forbidden)
            return
          }
          // Get Messages from index
          val messages = arrayListOf<String>()
          uniMessagesIndexManager!!.getEntriesFromIndexSearch(
                  searchText = "^${uniChatroom.uID}$", ixNr = 1, showAll = false, paginationIndex = pageIndex,
                  pageSize = pageSize, skip = skipCount) {
            it as UniMessage
            messages.add(uniMessagesIndexManager!!.encodeToJsonString(it))
          }
          appCall.respond(UniChatroomMessages(messages))
        }
      }
    }

    suspend fun addMemberToUniChatroom(
      appCall: ApplicationCall,
      config: UniChatroomAddMember
    ) {
      val uniChatroomGUID = appCall.parameters["uniChatroomGUID"]
      if (uniChatroomGUID.isNullOrEmpty()) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      with(UniChatroomController()) {
        mutex.withLock {
          val uniChatroom: UniChatroom? = getChatroom(uniChatroomGUID)
          if (uniChatroom == null) {
            appCall.respond(HttpStatusCode.NotFound)
            return
          }
          if (uniChatroom.checkIsMemberBanned(
                    username = getJWTEmail(appCall), isEmail = true)) {
            appCall.respond(HttpStatusCode.Forbidden)
            return
          }
          if (!uniChatroom.addOrUpdateMember(config.member, getRoleFromConfig(config.role))) {
            appCall.respond(HttpStatusCode.InternalServerError)
            return
          }
          saveChatroom(uniChatroom)
        }
        appCall.respond(HttpStatusCode.OK)
      }
    }

    suspend fun removeMemberOfUniChatroom(
      appCall: ApplicationCall,
      config: UniChatroomRemoveMember
    ) {
      val uniChatroomGUID = appCall.parameters["uniChatroomGUID"]
      if (uniChatroomGUID.isNullOrEmpty()) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      with(UniChatroomController()) {
        mutex.withLock {
          val uniChatroom: UniChatroom? = getChatroom(uniChatroomGUID)
          if (uniChatroom == null) {
            appCall.respond(HttpStatusCode.NotFound)
            return
          }
          val requestUsername = UserCLIManager.getUserFromEmail(getJWTEmail(appCall))!!.username
          if (uniChatroom.checkIsMemberBanned(requestUsername)) {
            appCall.respond(HttpStatusCode.Forbidden)
            return
          }
          if (!checkIsOwner(requestUsername, uniChatroom)) {
            appCall.respond(HttpStatusCode.Forbidden)
            return
          }
          if (!uniChatroom.removeMember(config.member)) {
            appCall.respond(HttpStatusCode.NotFound)
            return
          }
          saveChatroom(uniChatroom)
        }
        appCall.respond(HttpStatusCode.OK)
      }
    }

    suspend fun banMemberOfUniChatroom(
      appCall: ApplicationCall,
      config: UniChatroomRemoveMember
    ) {
      val uniChatroomGUID = appCall.parameters["uniChatroomGUID"]
      if (uniChatroomGUID.isNullOrEmpty()) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      with(UniChatroomController()) {
        mutex.withLock {
          val uniChatroom: UniChatroom? = getChatroom(uniChatroomGUID)
          if (uniChatroom == null) {
            appCall.respond(HttpStatusCode.NotFound)
            return
          }
          val requestUsername = UserCLIManager.getUserFromEmail(getJWTEmail(appCall))!!.username
          if (uniChatroom.checkIsMemberBanned(requestUsername)) {
            appCall.respond(HttpStatusCode.Forbidden)
            return
          }
          if (!checkIsOwner(requestUsername, uniChatroom)) {
            appCall.respond(HttpStatusCode.Forbidden)
            return
          }
          // Add member to the ban list to revoke all access rights
          if (!uniChatroom.banMember(config.member)) {
            appCall.respond(HttpStatusCode.NotFound)
            return
          }
          // Now remove him from the member list
          if (!uniChatroom.removeMember(config.member)) {
            appCall.respond(HttpStatusCode.NotFound)
            return
          }
          saveChatroom(uniChatroom)
        }
        appCall.respond(HttpStatusCode.OK)
      }
    }

    private fun getRoleFromConfig(role: String): UniRole {
      return UniRole(role)
    }

    suspend fun getMembersOfUniChatroom(appCall: ApplicationCall) {
      val uniChatroomGUID = appCall.parameters["uniChatroomGUID"]
      if (uniChatroomGUID.isNullOrEmpty()) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      with(UniChatroomController()) {
        val uniChatroom = getChatroom(uniChatroomGUID)
        if (uniChatroom == null) {
          appCall.respond(HttpStatusCode.NotFound)
        } else {
          if (uniChatroom.checkIsMemberBanned(
                    username = getJWTEmail(appCall), isEmail = true)) {
            appCall.respond(HttpStatusCode.Forbidden)
            return
          }
          appCall.respond(uniChatroom.members)
        }
      }
    }

    suspend fun addRoleToMemberOfUniChatroom(
      appCall: ApplicationCall,
      config: UniChatroomMemberRole?,
      chatroomConfig: UniChatroomRole?,
      isChatroom: Boolean = false,
      isRead: Boolean = false,
      isWrite: Boolean = false
    ) {
      if ((isChatroom && chatroomConfig == null) || (!isChatroom && config == null)) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      val uniChatroomGUID = appCall.parameters["uniChatroomGUID"]
      if (uniChatroomGUID.isNullOrEmpty()) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      if (isChatroom) {
        if (chatroomConfig != null && chatroomConfig.name.isEmpty()) {
          appCall.respond(HttpStatusCode.BadRequest)
          return
        }
      } else {
        if (config != null && config.member.isEmpty()) {
          appCall.respond(HttpStatusCode.BadRequest)
          return
        }
        if (config != null && config.role.isEmpty()) {
          appCall.respond(HttpStatusCode.BadRequest)
          return
        }
      }
      with(UniChatroomController()) {
        mutex.withLock {
          val uniChatroom: UniChatroom? = getChatroom(uniChatroomGUID)
          if (uniChatroom == null) {
            appCall.respond(HttpStatusCode.NotFound)
            return
          }
          if (uniChatroom.checkIsMemberBanned(
                    username = getJWTEmail(appCall), isEmail = true)) {
            appCall.respond(HttpStatusCode.Forbidden)
            return
          }
          if (isChatroom) {
            if (chatroomConfig != null) uniChatroom.addRole(chatroomConfig, isRead, isWrite)
          } else {
            if (config != null) {
              if (uniChatroom.addOrUpdateMember(config.member, UniRole(config.role))) {
                log(Log.Type.COM, "Role ${config.role} added to ${config.member}")
              }
            }
          }
          saveChatroom(uniChatroom)
        }
        appCall.respond(HttpStatusCode.OK)
      }
    }

    suspend fun removeRoleOfMemberOfUniChatroom(
      appCall: ApplicationCall,
      config: UniChatroomMemberRole?,
      chatroomConfig: UniChatroomRole?,
      isChatroom: Boolean = false,
      isRead: Boolean = false,
      isWrite: Boolean = false
    ) {
      if ((isChatroom && chatroomConfig == null) || (!isChatroom && config == null)) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      val uniChatroomGUID = appCall.parameters["uniChatroomGUID"]
      if (uniChatroomGUID.isNullOrEmpty()) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      if (isChatroom) {
        if (chatroomConfig != null && chatroomConfig.name.isEmpty()) {
          appCall.respond(HttpStatusCode.BadRequest)
          return
        }
      } else {
        if (config != null && config.member.isEmpty()) {
          appCall.respond(HttpStatusCode.BadRequest)
          return
        }
        if (config != null && config.role.isEmpty()) {
          appCall.respond(HttpStatusCode.BadRequest)
          return
        }
      }
      with(UniChatroomController()) {
        mutex.withLock {
          val uniChatroom: UniChatroom? = getChatroom(uniChatroomGUID)
          if (uniChatroom == null) {
            appCall.respond(HttpStatusCode.NotFound)
            return
          }
          if (uniChatroom.checkIsMemberBanned(
                    username = getJWTEmail(appCall), isEmail = true)) {
            appCall.respond(HttpStatusCode.Forbidden)
            return
          }
          if (isChatroom) {
            if (chatroomConfig != null) {
              uniChatroom.removeRole(chatroomConfig, isRead, isWrite)
            }
          } else {
            if (config != null) {
              for (uniMember in uniChatroom.members) {
                val member = Json.decodeFromString<UniMember>(uniMember)
                if (member.username == config.member) {
                  member.removeRole(UniRole(config.role))
                  break
                }
              }
            }
          }
          saveChatroom(uniChatroom)
        }
      }
    }

    fun getUsernameReversedBase(appCall: ApplicationCall): String {
      val username = UserCLIManager.getUserFromEmail(getJWTEmail(appCall))!!.username
      val usernameReversed = username.reversed()
      val usernameBase = Base64.getUrlEncoder().encodeToString(usernameReversed.toByteArray()).replace("=", "")
      return java.net.URLEncoder.encode(usernameBase, "utf-8")
    }

    suspend fun setFirebaseCloudMessagingSubscription(
      appCall: ApplicationCall,
      config: FirebaseCloudMessagingSubscription,
    ) {
      val uniChatroomGUID = appCall.parameters["uniChatroomGUID"]
      if (uniChatroomGUID.isNullOrEmpty()) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      with(UniChatroomController()) {
        mutex.withLock {
          val uniChatroom: UniChatroom? = getChatroom(uniChatroomGUID)
          if (uniChatroom == null) {
            appCall.respond(HttpStatusCode.NotFound)
            return
          }
          if (uniChatroom.checkIsMemberBanned(
                    username = getJWTEmail(appCall), isEmail = true)) {
            appCall.respond(HttpStatusCode.Forbidden)
            return
          }
          uniChatroom.addOrUpdateMember(
                  username = UserCLIManager.getUserFromEmail(getJWTEmail(appCall))!!.username,
                  fcmToken = config.fcmToken)
          saveChatroom(uniChatroom)
        }
        appCall.respond(HttpStatusCode.OK)
      }
    }

    suspend fun setPubKeyPEM(
      appCall: ApplicationCall,
      config: PubKeyPEMContainer,
    ) {
      val uniChatroomGUID = appCall.parameters["uniChatroomGUID"]
      if (uniChatroomGUID.isNullOrEmpty()) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      val usernameTokenEmail = getJWTEmail(appCall)
      val usernameToken = UserCLIManager.getUserFromEmail(usernameTokenEmail)!!.username
      with(UniChatroomController()) {
        mutex.withLock {
          val uniChatroom: UniChatroom? = getChatroom(uniChatroomGUID)
          if (uniChatroom == null) {
            appCall.respond(HttpStatusCode.NotFound)
            return
          }
          if (uniChatroom.checkIsMemberBanned(usernameToken) || !uniChatroom.checkIsMember(usernameToken)) {
            appCall.respond(HttpStatusCode.Forbidden)
            return
          }
          uniChatroom.addOrUpdateMember(
                  username = UserCLIManager.getUserFromEmail(getJWTEmail(appCall))!!.username,
                  pubKeyPEM = config.pubKeyPEM)
          saveChatroom(uniChatroom)
        }
        appCall.respond(HttpStatusCode.OK)
      }
    }

    suspend fun setUniChatroomImage(
      appCall: ApplicationCall,
      config: UniChatroomImage
    ) {
      val uniChatroomGUID = appCall.parameters["uniChatroomGUID"]
      if (uniChatroomGUID.isNullOrEmpty()) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      with(UniChatroomController()) {
        mutex.withLock {
          val uniChatroom: UniChatroom? = getChatroom(uniChatroomGUID)
          if (uniChatroom == null) {
            appCall.respond(HttpStatusCode.NotFound)
            return
          }
          if (uniChatroom.checkIsMemberBanned(
                    username = getJWTEmail(appCall), isEmail = true)) {
            appCall.respond(HttpStatusCode.Forbidden)
            return
          }
          with(SnippetBaseController()) {
            val snippet = saveFile(
                    filename = "", base64 = config.imageBase64, snippet = createSnippet(),
                    owner = "clarifier-$uniChatroomGUID", maxWidth = 300, maxHeight = 300)
            if (snippet == null) {
              appCall.respond(HttpStatusCode.InternalServerError)
              return
            }
            uniChatroom.imgGUID = snippet.guid
          }
          saveChatroom(uniChatroom)
        }
        appCall.respond(HttpStatusCode.OK)
      }
    }

    suspend fun setUniMemberImage(
      appCall: ApplicationCall,
      config: UniMemberProfileImage,
      isBanner: Boolean = false
    ) {
      val uniChatroomGUID = appCall.parameters["uniChatroomGUID"]
      if (uniChatroomGUID.isNullOrEmpty()) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      with(UniChatroomController()) {
        mutex.withLock {
          val uniChatroom: UniChatroom? = getChatroom(uniChatroomGUID)
          if (uniChatroom == null) {
            appCall.respond(HttpStatusCode.NotFound)
            return
          }
          if (uniChatroom.checkIsMemberBanned(
                    username = getJWTEmail(appCall), isEmail = true)) {
            appCall.respond(HttpStatusCode.Forbidden)
            return
          }
          with(SnippetBaseController()) {
            val maxWidth = if (isBanner) {
              1920
            } else {
              300
            }
            val maxHeight = if (isBanner) {
              1080
            } else {
              300
            }
            val snippet = saveFile(
                    filename = "", base64 = config.imageBase64, snippet = createSnippet(),
                    owner = getUsernameReversedBase(appCall), maxWidth = maxWidth, maxHeight = maxHeight)
            if (snippet == null) {
              appCall.respond(HttpStatusCode.InternalServerError)
              return
            }
            if (!isBanner) {
              // Did we save a GIF as the profile picture?
              if (snippet.payloadMimeType.contains("image/gif")) {
                // Yes => save it again as a .jpg to have both the animated and static images
                val snippetStatic = saveFile(
                        filename = "", base64 = config.imageBase64, snippet = createSnippet(),
                        owner = getUsernameReversedBase(appCall), forceFileExtension = "png",
                        forceMimeType = "data:image/png;base64")
                if (snippetStatic == null) {
                  appCall.respond(HttpStatusCode.InternalServerError)
                  return
                }
                uniChatroom.addOrUpdateMember(config.username, imageSnippetURL = snippetStatic.guid)
                uniChatroom.addOrUpdateMember(config.username, imageSnippetAnimatedURL = snippet.guid)
              } else {
                uniChatroom.addOrUpdateMember(config.username, imageSnippetURL = snippet.guid)
                uniChatroom.addOrUpdateMember(config.username, imageSnippetAnimatedURL = "")
              }
            }
            if (isBanner) uniChatroom.addOrUpdateMember(config.username, bannerSnippetURL = snippet.guid)
          }
          saveChatroom(uniChatroom)
        }
        appCall.respond(HttpStatusCode.OK)
      }
    }

    suspend fun createSnippetResource(
      appCall: ApplicationCall,
      payload: SnippetPayload
    ) {
      if (!UserCLIManager.checkModuleRight(getJWTEmail(appCall), "M6")) {
        appCall.respond(HttpStatusCode.Forbidden)
        return
      }
      // Check if file size does not exceed maximum
      /* We calculate the file size with the following formula:
          Math.Ceiling(base64 / 4) * 3 * 0.000001
        Explanation:
          base64 String is 133,33% bigger (usually) than the true file size
            -> we need to divide by 4 and multiply by 3
          We want to compare the MB size for convenience
            -> we multiply by 0.000001 to convert B to MB
       */
      var sizeInMB = ceil((payload.payload.length / 4).toDouble()) * 3 * 0.000001
      // Round to two decimal places
      sizeInMB = sizeInMB.toBigDecimal().setScale(2, RoundingMode.UP).toDouble()
      // We allow for a maximum of 20.0 MB for now
      if (sizeInMB > 20.0) {
        appCall.respond(HttpStatusCode.PayloadTooLarge)
        return
      }
      var srcUniChatroomUID = -1L
      var chatroom: UniChatroom? = null
      if (payload.chatroomGUID.isNotEmpty()) {
        with(UniChatroomController()) {
          chatroom = getChatroom(payload.chatroomGUID)
          if (chatroom == null) {
            appCall.respond(HttpStatusCode.NotFound)
            return
          }
          val username = UserCLIManager.getUserFromEmail(getJWTEmail(appCall))!!.username
          if (!chatroom!!.checkIsMember(username) || chatroom!!.checkIsMemberBanned(username)) {
            appCall.respond(HttpStatusCode.Forbidden)
            return
          }
          srcUniChatroomUID = chatroom!!.uID
        }
      }
      var srcWisdomUID = -1L
      val knowledgeController = KnowledgeController()
      var knowledge: Knowledge?
      var knowledgeChecked = false
      if (payload.wisdomGUID.isNotEmpty()) {
        with(WisdomController()) {
          val wisdom = getWisdomFromGUID(payload.wisdomGUID)
          if (wisdom == null) {
            appCall.respond(HttpStatusCode.NotFound)
            return
          }
          knowledge = knowledgeController.get(wisdom.knowledgeUID) as Knowledge
          if (!knowledgeController.httpCanAccessKnowledge(appCall, knowledge!!, chatroom)) return
          srcWisdomUID = wisdom.uID
          knowledgeChecked = true
        }
      }
      var srcProcessUID = -1L
      if (payload.processGUID.isNotEmpty()) {
        with(ProcessController()) {
          val process = getProcessFromGUID(payload.processGUID)
          if (process == null) {
            appCall.respond(HttpStatusCode.NotFound)
            return
          }
          if (!knowledgeChecked) {
            knowledge = knowledgeController.get(process.knowledgeUID) as Knowledge
            if (!knowledgeController.httpCanAccessKnowledge(appCall, knowledge!!, chatroom)) return
          }
          srcProcessUID = process.uID
        }
      }
      with(SnippetBaseController()) {
        val snippet = saveFile(
                filename = payload.name, base64 = payload.payload, snippet = createSnippet(),
                owner = getUsernameReversedBase(appCall), maxWidth = 1920, maxHeight = 1920, fileSize = sizeInMB,
                srcUniChatroomUID = srcUniChatroomUID, srcWisdomUID = srcWisdomUID, srcProcessUID = srcProcessUID)
        if (snippet == null) {
          appCall.respond(HttpStatusCode.InternalServerError)
          return
        }
        appCall.respond(Json.encodeToString(SnippetResponse(HttpStatusCode.Created.value, snippet.guid)))
      }
    }

    suspend fun getSnippetResource(
      appCall: ApplicationCall,
      delete: Boolean = false
    ) {
      val snippetGUID = appCall.parameters["snippetGUID"]
      if (snippetGUID.isNullOrEmpty()) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      with(SnippetBaseController()) {
        val snippet = getSnippet(snippetGUID)
        if (snippet == null) {
          appCall.respond(HttpStatusCode.NotFound)
          return
        }
        if (snippet.payloadType.contains("file")) {
          val fileToDownload = File(Paths.get(snippet.payload).toString())
          if (fileToDownload.isFile) {
            if (!delete) {
              appCall.response.header(
                      name = HttpHeaders.ContentDisposition, value = ContentDisposition.Attachment.withParameter(
                      key = ContentDisposition.Parameters.FileName, value = snippet.payloadName).toString())
              appCall.respondFile(fileToDownload)
              return
            } else {
              if (fileToDownload.delete()) {
                // Remove snippet from index
                snippet.guid = ""
                snippet.srcUniChatroomUID = -1L
                snippet.srcWisdomUID = -1L
                snippet.srcProcessUID = -1L
                saveResource(snippet)
                // Feedback
                appCall.respond(HttpStatusCode.OK)
                return
              } else {
                appCall.respond(HttpStatusCode.Conflict)
                return
              }
            }
          } else {
            appCall.respond(HttpStatusCode.NotFound)
            return
          }
        } else {
          appCall.respond(HttpStatusCode.BadRequest)
          return
        }
      }
    }

    suspend fun changeUsername(
      appCall: ApplicationCall,
      payload: UsernameChange
    ) {
      if (payload.username.isEmpty() || payload.newUsername.isEmpty()) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      if (UserCLIManager.changeUsername(getJWTEmail(appCall), payload)) {
        appCall.respond(HttpStatusCode.OK)
      } else {
        appCall.respond((HttpStatusCode.BadRequest))
      }
    }

    suspend fun changePassword(
      appCall: ApplicationCall,
      payload: PasswordChange
    ) {
      if (payload.username.isEmpty() || payload.password.isEmpty() || payload.newPassword.isEmpty()) {
        appCall.respond(HttpStatusCode.BadRequest)
        return
      }
      if (UserCLIManager.changePassword(getJWTEmail(appCall), payload)) {
        appCall.respond(HttpStatusCode.OK)
      } else {
        appCall.respond((HttpStatusCode.BadRequest))
      }
    }

    suspend fun getUsercount(appCall: ApplicationCall) {
      if (contactIndexManager == null) {
        appCall.respond(HttpStatusCode.ServiceUnavailable)
        return
      }
      appCall.respond(contactIndexManager!!.getLastUniqueID().toInt())
    }

    suspend fun getActiveMembersOfUniChatroom(
      appCall: ApplicationCall,
      uniChatroomGUID: String
    ) {
      with(UniChatroomController()) {
        val uniChatroom: UniChatroom? = getChatroom(uniChatroomGUID)
        if (uniChatroom == null) {
          appCall.respond(HttpStatusCode.NotFound)
          return
        }
        val username = getJWTEmail(appCall)
        if (uniChatroom.checkIsMemberBanned(
                  username = username, isEmail = true)) {
          appCall.respond(HttpStatusCode.Forbidden)
          return
        }
        val activeMembers = ActiveMembersPayload()
        val session = UniChatroomController.clarifierSessions[uniChatroomGUID] ?: return
        session.clarifierSessionUsers.forEach {
          if (!it.value.session.outgoing.isClosedForSend) {
            activeMembers.members.add(it.value.username)
          }
        }
        appCall.respond(activeMembers)
        return
      }
    }

    suspend fun getDirectChatrooms(
      appCall: ApplicationCall,
      username: String?,
      hasToBeJoined: Boolean = true
    ) {
      val chatrooms = UniChatroomController().getDirectChatrooms(appCall, username, hasToBeJoined)
      if (chatrooms.chatrooms.isNotEmpty()) {
        appCall.respond(chatrooms)
      } else {
        appCall.respond(HttpStatusCode.NotFound)
      }
    }
  }
}
