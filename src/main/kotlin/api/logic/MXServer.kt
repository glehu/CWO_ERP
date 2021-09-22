package api.logic

import api.gui.GSpotify
import api.misc.json.EntryJson
import api.misc.json.SpotifyAuthCallbackJson
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import modules.mx.dataPath
import modules.mx.logic.MXLog
import modules.mx.logic.MXUserManager
import modules.mx.m1GlobalIndex
import modules.mx.m2GlobalIndex
import modules.mx.m3GlobalIndex
import tornadofx.Controller
import java.io.File

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
                    if (userManager.login(credentials.name, credentials.password)) {
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
                log(MXLog.LogType.COM, "Spotify Auth Callback received")
                call.respondText("CWO ERP Spotify Authorization Callback Site")
                val code: String? = call.request.queryParameters["code"]
                find<GSpotify>().authCodeProperty.value = code
                find<GSpotify>().showTokenData(
                    SpotifyAUTH().getAccessTokenFromAuthCode(code!!) as SpotifyAuthCallbackJson
                )
            }
            //----------------------------------v
            //------------ CWO  API ------------|
            //----------------------------------^
            authenticate("auth-basic") {
                get("/login") {
                    log(MXLog.LogType.COM, "User ${call.principal<UserIdPrincipal>()?.name} login")
                    call.respond(
                        MXServerController.generateLoginResponse(
                            MXUserManager()
                                .getCredentials()
                                .credentials[call.principal<UserIdPrincipal>()?.name]!!
                        )
                    )
                }
                route("/") {
                    get {
                        call.respondFile(File("$dataPath\\data\\web\\home.html"))
                    }
                }
                route("/api")
                {
                    getIndexSelection(
                        m1GlobalIndex, m2GlobalIndex, m3GlobalIndex
                    )
                    getEntry(
                        m1GlobalIndex, m2GlobalIndex, m3GlobalIndex
                    )
                    saveEntry(
                        m1GlobalIndex, m2GlobalIndex, m3GlobalIndex
                    )
                    getEntryLock(
                        m1GlobalIndex, m2GlobalIndex, m3GlobalIndex
                    )
                    setEntryLock(
                        m1GlobalIndex, m2GlobalIndex, m3GlobalIndex
                    )
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
                call.respond(MXServerController.getEntry(call, ix))
            }
        }
    }

    private fun Route.saveEntry(vararg indexManager: IIndexManager) {
        for (ix in indexManager) {
            post("${ix.module.lowercase()}/saveentry") {
                val entryJson: EntryJson = call.receive()
                call.respond(
                    MXServerController.saveEntry(
                        entryJson.entry,
                        ix,
                        call.principal<UserIdPrincipal>()?.name!!
                    )
                )
            }
        }
    }

    private fun Route.getEntryLock(vararg indexManager: IIndexManager) {
        for (ix in indexManager) {
            get("${ix.module.lowercase()}/getentrylock/{searchString}") {
                call.respond(MXServerController.getEntryLock(call, ix))
            }
        }
    }

    private fun Route.setEntryLock(vararg indexManager: IIndexManager) {
        for (ix in indexManager) {
            get("${ix.module.lowercase()}/setentrylock/{searchString}") {
                call.respond(
                    MXServerController.setEntryLock(
                        call,
                        ix
                    )
                )
            }
        }
    }

    init {
        runAsync {
            serverEngine.start(wait = true)
        }
    }
}