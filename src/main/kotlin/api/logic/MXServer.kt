package api.logic

import api.gui.GSpotify
import api.misc.json.EntryJson
import api.misc.json.SpotifyAuthCallbackJson
import api.misc.json.UPPriceCategoryJson
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.application.*
import io.ktor.auth.*
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
import kotlinx.serialization.json.Json
import modules.m4.logic.M4PriceManager
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
                get("/login") {
                    log(MXLog.LogType.COM, "User ${call.principal<UserIdPrincipal>()?.name} login")
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
                    log(MXLog.LogType.COM, "User ${call.principal<UserIdPrincipal>()?.name} logout")
                    MXUserManager().setUserOnlineStatus(
                        username = call.principal<UserIdPrincipal>()?.name!!,
                        online = false
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
                //----------------------------------v
                //------------ CWO  API ------------|
                //----------------------------------^
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

                    /**
                     * M3 Endpoints (Invoice)
                     */
                    addWebshopOrder()

                    /**
                     * M4 Endpoints (Item)
                     */
                    getPriceCategories()
                    getPriceCategoryNumber()
                    savePriceCategory()
                    deletePriceCategory()
                }
            }
        }
    }

    private fun Route.getIndexSelection(vararg indexManager: IIndexManager) {
        for (ix in indexManager) {
            get("${ix.module.lowercase()}/indexselection") {
                call.respond(ix.getIndexUserSelection())
            }
        }
    }

    private fun Route.getEntry(vararg indexManager: IIndexManager) {
        for (ix in indexManager) {
            get("${ix.module.lowercase()}/entry/{searchString}") {
                call.respond(
                    MXServerController.getEntry(
                        appCall = call,
                        indexManager = ix
                    )
                )
            }
        }
    }

    private fun Route.saveEntry(vararg indexManager: IIndexManager) {
        for (ix in indexManager) {
            post("${ix.module.lowercase()}/saveentry") {
                val entryJson: EntryJson = call.receive()
                call.respond(
                    MXServerController.saveEntry(
                        entry = entryJson.entry,
                        indexManager = ix,
                        username = call.principal<UserIdPrincipal>()?.name!!
                    )
                )
            }
        }
    }

    private fun Route.getEntryLock(vararg indexManager: IIndexManager) {
        for (ix in indexManager) {
            get("${ix.module.lowercase()}/getentrylock/{searchString}") {
                call.respond(
                    MXServerController.getEntryLock(
                        appCall = call,
                        indexManager = ix
                    )
                )
            }
        }
    }

    private fun Route.setEntryLock(vararg indexManager: IIndexManager) {
        for (ix in indexManager) {
            get("${ix.module.lowercase()}/setentrylock/{searchString}") {
                call.respond(
                    MXServerController.setEntryLock(
                        appCall = call,
                        indexManager = ix
                    )
                )
            }
        }
    }

    private fun Route.addWebshopOrder() {
        get("m3/neworder/{itemID}") {
            call.respond(MXServerController.placeWebshopOrder(call))
        }
    }

    private fun Route.getPriceCategories() {
        get("m4/pricecategories") {
            call.respond(
                M4PriceManager().getCategories()
            )
        }
    }

    private fun Route.getPriceCategoryNumber() {
        get("m4/categorynumber") {
            call.respond(
                M4PriceManager().getNumber(M4PriceManager().getCategories())
            )
        }
    }

    private fun Route.savePriceCategory() {
        post("m4/savecategory") {
            call.respond(MXServerController.updatePriceCategories(call.receive() as UPPriceCategoryJson))
        }
    }

    private fun Route.deletePriceCategory() {
        post("m4/deletecategory") {
            call.respond(MXServerController.deletePriceCategory(call.receive() as UPPriceCategoryJson))
        }
    }

    init {
        serverJobGlobal = GlobalScope.launch {
            serverEngine.start(wait = true)
        }
    }
}
