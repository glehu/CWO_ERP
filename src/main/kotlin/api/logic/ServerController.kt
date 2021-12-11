package api.logic

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
import modules.m4.logic.ItemStorageManager
import modules.mx.*
import modules.mx.logic.*
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.set

@ExperimentalSerializationApi
class ServerController {
    @InternalAPI
    @ExperimentalSerializationApi
    companion object Server : IModule {
        override val moduleNameLong = "MXServerController"
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
            val expiresInMs = (1 * 60 * 60 * 1000)
            val expiresAt = Date(System.currentTimeMillis() + expiresInMs)
            val token = JWT.create()
                .withAudience("http://localhost:8000/")
                .withIssuer("http://localhost:8000/")
                .withClaim("username", user.username)
                .withExpiresAt(expiresAt)
                .sign(Algorithm.HMAC256(iniVal.token))
            log(Log.LogType.COM, "Token generated for ${user.username}", "/login")
            val loginResponse = Json.encodeToString(
                LoginResponseJson(
                    httpCode = 200,
                    token = token,
                    expiresInMs = expiresInMs,
                    accessM1 = user.canAccessM1,
                    accessM2 = user.canAccessM2,
                    accessM3 = user.canAccessM3,
                    accessM4 = user.canAccessM4
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
            if (body.itemUIDs.size != -1) {
                for (i in 0 until body.itemUIDs.size) {
                    val item = m4GlobalIndex!!.get(body.itemUIDs[i]) as Item
                    if (item.uID != -1) {
                        /**
                         * Item position
                         */
                        val itemPosition = InvoicePosition(item.uID, item.description)
                        itemPosition.grossPrice = Json.decodeFromString<ItemPriceCategory>(item.prices[0]!!).grossPrice
                        itemPosition.userName = activeUser.username
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
                val contactsMatched = m2GlobalIndex!!.getEntryBytesListJson(
                    searchText = userName,
                    ixNr = 1,
                    exactSearch = false
                )
                if (contactsMatched.resultsList.isEmpty() && m3IniVal.autoCreateContacts) {
                    val contact = Contact(-1, userName)
                    contact.email = userName
                    contact.moneySent = order.netTotal
                    mutex.withLock {
                        order.buyerUID = m2GlobalIndex!!.save(contact)
                    }
                } else {
                    val contact = m2GlobalIndex!!.decode(contactsMatched.resultsList[0]) as Contact
                    order.buyerUID = contact.uID
                    if (contact.email.isEmpty() || contact.email == "?") contact.email = userName
                    contact.moneySent = order.netTotal
                    mutex.withLock {
                        m2GlobalIndex!!.save(contact)
                    }
                }
                order.seller = "<Self>"
                if (m3IniVal.autoSendEMailConfirmation) order.emailConfirmationSent = true
                mutex.withLock {
                    m3GlobalIndex!!.save(entry = order, userName = userName)
                }
                mutex.withLock {
                    log(
                        logType = Log.LogType.COM,
                        text = "web shop order #${order.uID} from ${order.buyer}",
                        apiEndpoint = appCall.request.uri,
                        moduleAlt = m3GlobalIndex!!.module
                    )
                }
            }
            if (m3IniVal.autoSendEMailConfirmation) {
                EMailer().sendEMail(
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
            val userManager = MXUserManager()
            var exists = false
            var success = true
            var message = ""
            mutex.withLock {
                val credentials = userManager.getCredentials()
                for (user in credentials.credentials) {
                    if (user.value.username.uppercase() == registrationPayload.username.uppercase()) {
                        exists = true
                    }
                }
                if (exists) {
                    success = false
                    message = "User already exists"
                } else {
                    val user = User(registrationPayload.username, encryptAES(registrationPayload.password))
                    userManager.updateUser(user, user, credentials)
                    log(Log.LogType.COM, "User ${registrationPayload.username} registered.", appCall.request.uri)
                }
            }
            return RegistrationResponse(success, message)
        }

        fun getItemImage(): String {
            val sampleImg = File("$dataPath\\data\\img\\orochi_logo_red_500x500.png")
            return Base64.getEncoder().encodeToString(sampleImg.readBytes())
        }

        fun getJWTUsername(appCall: ApplicationCall): String {
            return appCall.principal<JWTPrincipal>()!!.payload.getClaim("username").asString()
        }
    }
}
