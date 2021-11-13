package api.logic

import api.misc.json.*
import com.sultanofcardio.models.Email
import com.sultanofcardio.models.MailServer
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.request.*
import io.ktor.util.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m2.M2Contact
import modules.m3.M3Invoice
import modules.m3.M3InvoicePosition
import modules.m3.logic.M3CLIController
import modules.m3.logic.M3Controller
import modules.m4.M4Item
import modules.m4.M4PriceCategory
import modules.m4.logic.M4PriceManager
import modules.mx.*
import modules.mx.logic.MXLog
import modules.mx.logic.MXUserManager
import modules.mx.logic.encryptAES
import modules.mx.logic.encryptKeccak
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.set

@ExperimentalSerializationApi
class MXServerController {
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
                    logType = MXLog.LogType.COM,
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
                                userName = appCall.principal<UserIdPrincipal>()?.name!!
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
                    userName = appCall.principal<UserIdPrincipal>()?.name!!
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
                        userName = appCall.principal<UserIdPrincipal>()?.name!!
                    )
                }
            } else false
            return success
        }

        @InternalAPI
        fun generateLoginResponse(user: MXUser): ValidationContainerJson {
            val loginResponse = Json.encodeToString(
                LoginResponseJson(
                    httpCode = 200,
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
        fun updatePriceCategories(categoryJson: UPPriceCategoryJson): Boolean {
            M4PriceManager().updateCategory(
                categoryNew = Json.decodeFromString(categoryJson.catNew),
                categoryOld = Json.decodeFromString(categoryJson.catOld)
            )
            return true
        }

        @InternalAPI
        fun deletePriceCategory(categoryJson: UPPriceCategoryJson): Boolean {
            M4PriceManager().deleteCategory(
                category = Json.decodeFromString(categoryJson.catNew),
            )
            return true
        }

        suspend fun placeWebshopOrder(appCall: ApplicationCall): Int {
            val m3IniVal = M3Controller().getIni()

            /**
             * The webshop order
             */
            val order = M3Invoice(-1)

            /**
             * The ordered item's UID
             */
            val body = appCall.receive<WebshopOrder>()
            if (body.itemUIDs.size != -1) {
                for (i in 0 until body.itemUIDs.size) {
                    val item = m4GlobalIndex!!.get(body.itemUIDs[i]) as M4Item
                    if (item.uID != -1) {
                        /**
                         * Item position
                         */
                        val itemPosition = M3InvoicePosition(item.uID, item.description)
                        itemPosition.grossPrice = Json.decodeFromString<M4PriceCategory>(item.prices[0]!!).grossPrice
                        itemPosition.userName = activeUser.username
                        order.items[i] = Json.encodeToString(itemPosition)
                    }
                }
                /**
                 * Finalize the order and save it
                 */
                val userName = appCall.principal<UserIdPrincipal>()?.name!!
                M3CLIController().calculate(order)
                order.date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                order.text = "Web Order"
                order.buyer = userName
                //Check if the customer is an existing contact, if not, create it
                val contactsMatched = m2GlobalIndex!!.getEntryBytesListJson(
                    searchText = userName,
                    ixNr = 1,
                    exactSearch = false
                )
                if (contactsMatched.resultsList.isEmpty() && m3IniVal.autoCreateContacts) {
                    val contact = M2Contact(-1, userName)
                    contact.moneySent = order.netTotal
                    mutex.withLock {
                        order.buyerUID = m2GlobalIndex!!.save(contact)
                    }
                } else {
                    val contact = m2GlobalIndex!!.decode(contactsMatched.resultsList[0]) as M2Contact
                    order.buyerUID = contact.uID
                    contact.moneySent = order.netTotal
                    mutex.withLock {
                        m2GlobalIndex!!.save(contact)
                    }
                }
                order.seller = "<Self>"
                mutex.withLock {
                    log(
                        logType = MXLog.LogType.COM,
                        text = "web shop order #${order.uID} from ${order.buyer}",
                        apiEndpoint = appCall.request.uri,
                        moduleAlt = m3GlobalIndex!!.module
                    )
                }
                mutex.withLock {
                    m3GlobalIndex!!.save(entry = order, userName = userName)
                }
            }
            val jsonSerializer = Json {
                prettyPrint = true
            }
            val iniVal = jsonSerializer.decodeFromString<MXIni>(getIniFile().readText())
            MailServer(
                host = iniVal.emailHost,
                port = iniVal.emailPort,
                username = iniVal.emailUsername,
                password = iniVal.emailPassword
            ).sendEmail(
                Email(
                    from = "orochi@batsuzoku.eg",
                    subject = "Web Shop Order #${order.uID}",
                    body = "Hey, we're confirming your order over ${order.grossTotal} Euro.\n" +
                            "Order Number: #${order.uID}\n" +
                            "Date: ${order.date}",
                    recipient = order.buyer
                )
            )
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
                    val user = MXUser(registrationPayload.username, encryptAES(registrationPayload.password))
                    userManager.updateUser(user, user, credentials)
                    log(MXLog.LogType.COM, "User ${registrationPayload.username} registered.", appCall.request.uri)
                }
            }
            return RegistrationResponse(success, message)
        }

        fun getItemImage(): String {
            val sampleImg = File("$dataPath\\data\\img\\orochi_logo_red_500x500.png")
            return Base64.getEncoder().encodeToString(sampleImg.readBytes())
        }
    }
}
