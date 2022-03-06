package api.logic.core

import api.misc.json.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.request.*
import io.ktor.util.*
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
import modules.mx.*
import modules.mx.logic.*
import java.io.File
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.set

@ExperimentalSerializationApi
class ServerController {
  @InternalAPI
  @ExperimentalSerializationApi
  companion object Server : IModule {
    override val moduleNameLong = "ServerController"
    override val module = "MX"
    override fun getIndexManager(): IIndexManager? {
      return null
    }

    private val mutex = Mutex()

    suspend fun saveEntry(entry: ByteArray, indexManager: IIndexManager, username: String): Int {
      val uID: Int
      mutex.withLock {
        log(
          logType = Log.LogType.COM,
          text = "API ${indexManager.module} entry save",
          apiEndpoint = "/api/${indexManager.module}/save",
          moduleAlt = indexManager.module
        )
        uID = indexManager.save(
          entry = indexManager.decode(entry),
          userName = username
        )
      }
      return uID
    }

    fun getEntry(appCall: ApplicationCall, indexManager: IIndexManager): Any {
      val routePar = appCall.parameters["searchString"]
      if (routePar != null && routePar.isNotEmpty()) {
        when (appCall.request.queryParameters["type"]) {
          "uid" -> {
            val doLock = when (appCall.request.queryParameters["lock"]) {
              "true" -> true
              else -> false
            }
            return if (appCall.request.queryParameters["format"] == "json") {
              val entry = indexManager.decode(
                indexManager.getBytes(uID = routePar.toInt())
              )
              entry.initialize()
              indexManager.encodeToJsonString(
                entry = entry,
                prettyPrint = true
              )
            } else {
              indexManager.getBytes(
                uID = routePar.toInt(),
                lock = doLock,
                userName = appCall.principal<JWTPrincipal>()!!.payload.getClaim("username").asString()
              )
            }
          }
          "name" -> {
            val requestIndex = appCall.request.queryParameters["index"]
            val ixNr: Int = (requestIndex?.toInt()) ?: 1
            val exactSearch = requestIndex != null
            return if (appCall.request.queryParameters["format"] == "json") {
              indexManager.getEntryListJson(
                searchText = routePar,
                ixNr,
                exactSearch = exactSearch
              )
            } else {
              indexManager.getEntryBytesListJson(
                searchText = routePar,
                ixNr,
                exactSearch = exactSearch
              )
            }
          }
          else -> return ""
        }
      }
      return ""
    }

    fun getEntryLock(appCall: ApplicationCall, indexManager: IIndexManager): Boolean {
      val routePar = appCall.parameters["searchString"]
      if (routePar != null && routePar.isNotEmpty()) {
        return indexManager.getEntryLock(
          uID = routePar.toInt(),
          userName = appCall.principal<JWTPrincipal>()!!.payload.getClaim("username").asString()
        )
      }
      return false
    }

    suspend fun setEntryLock(appCall: ApplicationCall, indexManager: IIndexManager): Boolean {
      val routePar = appCall.parameters["searchString"]
      val queryPar = appCall.request.queryParameters["type"]
      val success: Boolean = if (routePar != null && routePar.isNotEmpty()) {
        mutex.withLock {
          indexManager.setEntryLock(
            uID = routePar.toInt(),
            doLock = queryPar.toBoolean(),
            userName = appCall.principal<JWTPrincipal>()!!.payload.getClaim("username").asString()
          )
        }
      } else false
      return success
    }

    @InternalAPI
    fun generateLoginResponse(user: User): ValidationContainerJson {
      val iniVal = Json.decodeFromString<Ini>(getIniFile().readText())
      //               Hours
      val expiresInMs = (1 * 60 * 60 * 1000)
      val expiresAt = Date(System.currentTimeMillis() + expiresInMs)
      val token = JWT.create()
        .withAudience("http://${iniVal.serverIPAddress}/")
        .withIssuer("http://${iniVal.serverIPAddress}/")
        .withClaim("username", user.username)
        .withExpiresAt(expiresAt)
        .sign(Algorithm.HMAC256(iniVal.token))
      log(Log.LogType.COM, "Token generated for ${user.username}", "/login")
      val loginResponse = Json.encodeToString(
        LoginResponseJson(
          httpCode = 200,
          token = token,
          expiresInMs = expiresInMs,
          accessM1 = user.canAccessDiscography,
          accessM2 = user.canAccessContacts,
          accessM3 = user.canAccessInvoices,
          accessM4 = user.canAccessInventory
        )
      )
      return ValidationContainerJson(
        contentJson = loginResponse,
        hash = encryptKeccak(
          input = loginResponse,
          salt = encryptKeccak(user.username),
          pepper = encryptKeccak("CWO_ERP LoginValidation")
        )
      )
    }

    @InternalAPI
    fun updatePriceCategories(categoryJson: ListDeltaJson): Boolean {
      ItemPriceManager().updateCategory(
        categoryNew = Json.decodeFromString(categoryJson.listEntryNew),
        categoryOld = Json.decodeFromString(categoryJson.listEntryOld)
      )
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
        storageOld = Json.decodeFromString(categoryJson.listEntryOld)
      )
      return true
    }

    @InternalAPI
    fun deleteStorage(categoryJson: ListDeltaJson): Boolean {
      ItemStorageManager().deleteStorage(
        storage = Json.decodeFromString(categoryJson.listEntryNew),
      )
      return true
    }

    suspend fun placeWebshopOrder(appCall: ApplicationCall): Int {
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
        if (item.uID != -1) {
          /**
           * Item position
           */
          val itemPosition = InvoicePosition(item.uID, item.description)
          itemPosition.grossPrice = Json.decodeFromString<ItemPriceCategory>(item.prices[0]!!).grossPrice
          itemPosition.amount = body.cart[i].amount.toDouble()
          order.items[i] = Json.encodeToString(itemPosition)
        }
      }
      order.customerNote = body.customerNote
      /**
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
        searchText = userName,
        ixNr = 1,
        exactSearch = false
      )
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
          logType = Log.LogType.COM,
          text = "web shop order #${order.uID} from ${order.buyer}",
          apiEndpoint = appCall.request.uri,
          moduleAlt = invoiceIndexManager!!.module
        )
      }
      if (m3IniVal.autoSendEmailConfirmation) {
        Emailer().sendEmail(
          subject = "Web Shop Order #${order.uID}",
          body = "Hey, we're confirming your order over ${order.grossTotal} Euro.\n" +
                  "Order Number: #${order.uID}\n" +
                  "Date: ${order.date}",
          recipient = order.buyer
        )
      }
      return order.uID
    }

    suspend fun registerUser(appCall: ApplicationCall): RegistrationResponse {
      val registrationPayload = appCall.receive<RegistrationPayload>()
      val userManager = UserCLIManager()
      var exists = false
      var isSuccess = true
      var message = ""
      mutex.withLock {
        val credentials = userManager.getCredentials()
        for (user in credentials.credentials) {
          if (user.value.username.uppercase() == registrationPayload.username.uppercase()) {
            exists = true
          }
        }
        if (exists) {
          isSuccess = false
          message = "User already exists"
        } else {
          val user = User(registrationPayload.username, encryptAES(registrationPayload.password))
          userManager.updateUser(user, user, credentials)
          log(Log.LogType.COM, "User ${registrationPayload.username} registered.", appCall.request.uri)
        }
      }
      return RegistrationResponse(isSuccess, message)
    }

    fun getItemImage(): String {
      val sampleImg = File(Paths.get(dataPath,"data","img","orochi_logo_red_500x500.png").toString())
      return Base64.getEncoder().encodeToString(sampleImg.readBytes())
    }

    fun getJWTUsername(appCall: ApplicationCall): String {
      return appCall.principal<JWTPrincipal>()!!.payload.getClaim("username").asString()
    }

    fun getOwnInvoices(appCall: ApplicationCall): Any {
      return invoiceIndexManager!!.getEntryListJson(
        searchText = getJWTUsername(appCall),
        ixNr = 2,
        exactSearch = true
      )
    }

    fun checkStorage(request: TwoIntOneDoubleJson): Boolean {
      return ItemStockPostingController().check(request.first, request.second, request.third)
    }

    fun getAvailableStock(request: PairIntJson): Double {
      return ItemStockPostingController().getAvailableStock(request.first, request.second)
    }
  }
}
