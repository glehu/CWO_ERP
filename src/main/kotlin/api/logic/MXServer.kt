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
import modules.m4.logic.M4PriceManager
import modules.m4.logic.M4StorageManager
import modules.mx.*
import modules.mx.gui.MGXDashboard
import modules.mx.logic.MXLog
import modules.mx.logic.MXUserManager
import tornadofx.Controller
import java.io.File

@DelicateCoroutinesApi
@InternalAPI
@ExperimentalSerializationApi
class MXServer : IModule, Controller() {
    override val moduleNameLong = "Server"
    override val module = "MX"
    override fun getIndexManager(): IIndexManager? {
        return null
    }

    private val iniVal = Json.decodeFromString<MXIni>(getIniFile().readText())
    private val userManager: MXUserManager by inject()
    lateinit var text: String

    val serverEngine = embeddedServer(Netty, port = 8000) {
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
                        .withAudience("http://localhost:8000/")
                        .withIssuer("http://localhost:8000/")
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
            get("/authcallback/spotify") {
                val code: String? = call.request.queryParameters["code"]
                if (code != null) {
                    call.respondFile(File("$dataPath\\data\\web\\spotifyCallback.html"))
                    log(MXLog.LogType.COM, "Spotify Auth Callback received")
                    val spotifyAPI = find<GSpotify>()
                    spotifyAPI.authCodeProperty.value = code
                    spotifyAPI.showTokenData(
                        SpotifyAUTH().getAccessTokenFromAuthCode(code) as SpotifyAuthCallbackJson
                    )
                    spotifyAPI.updateUserData()
                }
            }
            authenticate("auth-basic") {
                register()
                get("/login") {
                    log(
                        MXLog.LogType.COM,
                        "User ${call.principal<UserIdPrincipal>()?.name} login",
                        call.request.uri
                    )
                    val userManager = MXUserManager()
                    userManager.setUserOnlineStatus(
                        username = call.principal<UserIdPrincipal>()?.name!!,
                        online = true
                    )
                    call.respond(
                        MXServerController.generateLoginResponse(
                            userManager
                                .getCredentials()
                                .credentials[call.principal<UserIdPrincipal>()?.name]!!
                        )
                    )
                    if (!cliMode) {
                        find<MGXDashboard>().activeUsers.items = userManager.getActiveUsers()
                        find<MGXDashboard>().activeUsers.refresh()
                    }
                }
                get("/logout") {
                    MXUserManager().setUserOnlineStatus(
                        username = call.principal<UserIdPrincipal>()?.name!!,
                        online = false
                    )
                    log(
                        MXLog.LogType.COM,
                        "User ${call.principal<UserIdPrincipal>()?.name} logout",
                        call.request.uri
                    )
                    if (!cliMode) {
                        find<MGXDashboard>().activeUsers.items = userManager.getActiveUsers()
                        find<MGXDashboard>().activeUsers.refresh()
                    }
                }
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
                        log(
                            MXLog.LogType.COM,
                            text,
                            call.request.uri
                        )
                        call.respondText(text)
                    }
                }
                //------------------------------------------------------v
                //------------ CWO API, now with JWT AUTH! -------------|
                //------------------------------------------------------^
            }
            authenticate("auth-jwt") {
                get("/tokenremainingtime") {
                    val principal = call.principal<JWTPrincipal>()
                    val expiresInMs = principal!!.expiresAt?.time?.minus(System.currentTimeMillis())
                    call.respondText(expiresInMs.toString())
                }
                route("/api")
                {
                    /**
                     * General Endpoints
                     */
                    getIndexSelection(
                        m1GlobalIndex!!, m2GlobalIndex!!, m3GlobalIndex!!, m4GlobalIndex!!
                    )
                    getEntry(
                        m1GlobalIndex!!, m2GlobalIndex!!, m3GlobalIndex!!, m4GlobalIndex!!
                    )
                    saveEntry(
                        m1GlobalIndex!!, m2GlobalIndex!!, m3GlobalIndex!!, m4GlobalIndex!!
                    )
                    getEntryLock(
                        m1GlobalIndex!!, m2GlobalIndex!!, m3GlobalIndex!!, m4GlobalIndex!!
                    )
                    setEntryLock(
                        m1GlobalIndex!!, m2GlobalIndex!!, m3GlobalIndex!!, m4GlobalIndex!!
                    )
                    sendEMail()
                    getSettingsFileText()

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

    private fun Route.userTracking() {
        post("utr") {
            call.respond(usageTracker.writeUsageTrackingData(call))
        }
    }

    private fun Route.register() {
        post("register") {
            call.respond(MXServerController.registerUser(call))
        }
    }

    private fun Route.getIndexSelection(vararg indexManager: IIndexManager) {
        for (ix in indexManager) {
            get("${ix.module.lowercase()}/indexselection") {
                if (!userManager.checkModuleRight(MXServerController.getJWTUsername(call), ix.module)) {
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
                if (!userManager.checkModuleRight(MXServerController.getJWTUsername(call), ix.module)) {
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    call.respond(
                        MXServerController.getEntry(
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
                if (!userManager.checkModuleRight(MXServerController.getJWTUsername(call), ix.module)) {
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    val entryJson: EntryJson = call.receive()
                    call.respond(
                        MXServerController.saveEntry(
                            entry = entryJson.entry,
                            indexManager = ix,
                            username = MXServerController.getJWTUsername(call)
                        )
                    )
                }
            }
        }
    }

    private fun Route.getEntryLock(vararg indexManager: IIndexManager) {
        for (ix in indexManager) {
            get("${ix.module.lowercase()}/getentrylock/{searchString}") {
                if (!userManager.checkModuleRight(MXServerController.getJWTUsername(call), ix.module)) {
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    call.respond(
                        MXServerController.getEntryLock(
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
                if (!userManager.checkModuleRight(MXServerController.getJWTUsername(call), ix.module)) {
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    call.respond(
                        MXServerController.setEntryLock(
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
            if (!userManager.checkModuleRight(MXServerController.getJWTUsername(call), "M3")) {
                call.respond(HttpStatusCode.Forbidden)
            } else {
                call.respond(MXServerController.placeWebshopOrder(call))
            }
        }
    }

    private fun Route.getPriceCategories() {
        get("m4/pricecategories") {
            if (!userManager.checkModuleRight(MXServerController.getJWTUsername(call), "M4")) {
                call.respond(HttpStatusCode.Forbidden)
            } else {
                call.respond(
                    M4PriceManager().getCategories()
                )
            }
        }
    }

    private fun Route.getPriceCategoryNumber() {
        get("m4/categorynumber") {
            if (!userManager.checkModuleRight(MXServerController.getJWTUsername(call), "M4")) {
                call.respond(HttpStatusCode.Forbidden)
            } else {
                call.respond(
                    M4PriceManager().getNumber(M4PriceManager().getCategories())
                )
            }
        }
    }

    private fun Route.savePriceCategory() {
        post("m4/savecategory") {
            if (!userManager.checkModuleRight(MXServerController.getJWTUsername(call), "M4")) {
                call.respond(HttpStatusCode.Forbidden)
            } else {
                call.respond(MXServerController.updatePriceCategories(call.receive() as ListDeltaJson))
            }
        }
    }

    private fun Route.deletePriceCategory() {
        post("m4/deletecategory") {
            if (!userManager.checkModuleRight(MXServerController.getJWTUsername(call), "M4")) {
                call.respond(HttpStatusCode.Forbidden)
            } else {
                call.respond(MXServerController.deletePriceCategory(call.receive() as ListDeltaJson))
            }
        }
    }

    private fun Route.getStorages() {
        get("m4/storages") {
            if (!userManager.checkModuleRight(MXServerController.getJWTUsername(call), "M4")) {
                call.respond(HttpStatusCode.Forbidden)
            } else {
                call.respond(
                    M4StorageManager().getStorages()
                )
            }
        }
    }

    private fun Route.getStorageNumber() {
        get("m4/storagenumber") {
            if (!userManager.checkModuleRight(MXServerController.getJWTUsername(call), "M4")) {
                call.respond(HttpStatusCode.Forbidden)
            } else {
                call.respond(
                    M4StorageManager().getNumber(M4StorageManager().getStorages())
                )
            }
        }
    }

    private fun Route.saveStorage() {
        post("m4/savestorage") {
            if (!userManager.checkModuleRight(MXServerController.getJWTUsername(call), "M4")) {
                call.respond(HttpStatusCode.Forbidden)
            } else {
                call.respond(MXServerController.updateStorages(call.receive() as ListDeltaJson))
            }
        }
    }

    private fun Route.deleteStorage() {
        post("m4/deletestorage") {
            if (!userManager.checkModuleRight(MXServerController.getJWTUsername(call), "M4")) {
                call.respond(HttpStatusCode.Forbidden)
            } else {
                call.respond(MXServerController.deleteStorage(call.receive() as ListDeltaJson))
            }
        }
    }

    private fun Route.getItemImage() {
        get("m4/getimage/{itemUID}") {
            if (!userManager.checkModuleRight(MXServerController.getJWTUsername(call), "M4")) {
                call.respond(HttpStatusCode.Forbidden)
            } else {
                call.respond(MXServerController.getItemImage())
            }
        }
    }

    private fun Route.sendEMail() {
        //TODO: Needs to check rights (currently there are no email rights)
        post("sendemail") {
            val body = call.receive<EMailJson>()
            sendEMail(body.subject, body.body, body.recipient)
            call.respond(true)
        }
    }

    private fun Route.getSettingsFileText() {
        post("getsettingsfiletext") {
            val body: SettingsRequestJson = Json.decodeFromString(call.receive())
            if (!userManager.checkModuleRight(MXServerController.getJWTUsername(call), body.module)) {
                call.respond(HttpStatusCode.Forbidden)
            } else {
                call.respond(
                    MXServerController.getSettingsFileText(
                        moduleShort = body.module,
                        subSetting = body.subSetting
                    )
                )
            }
        }
    }

    init {
        serverJobGlobal = GlobalScope.launch {
            serverEngine.start(wait = true)
        }
    }
}
