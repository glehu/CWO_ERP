package api.logic

import api.gui.GSpotify
import api.misc.json.EntryJson
import api.misc.json.SpotifyAuthCallbackJson
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import modules.m1.Song
import modules.m2.Contact
import modules.m3.Invoice
import modules.mx.logic.MXLog
import modules.mx.logic.MXUserManager
import modules.mx.m1GlobalIndex
import modules.mx.m2GlobalIndex
import modules.mx.m3GlobalIndex
import tornadofx.Controller

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
        routing {
            authenticate("auth-basic") {
                get("/login") {
                    log(MXLog.LogType.COM, "User ${call.principal<UserIdPrincipal>()?.name} login")
                    call.respond(true)
                }
            }
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
                get("/") {
                    log(MXLog.LogType.COM, "User ${call.principal<UserIdPrincipal>()?.name} Connected")
                    call.respondText("Hello, ${call.principal<UserIdPrincipal>()?.name}!")
                }
                route("/api")
                {
                    route("/m1") {
                        get("/indexselection") {
                            call.respond(m1GlobalIndex.getIndexUserSelection())
                        }
                        get("/entry/{searchString}") {
                            val routePar = call.parameters["searchString"]
                            if (routePar != null && routePar.isNotEmpty()) {
                                val queryPar = call.request.queryParameters["type"]
                                if (queryPar == "uid") {
                                    call.respond(m1GlobalIndex.getBytes(routePar.toInt()))
                                } else if (queryPar == "name") {
                                    call.respond(m1GlobalIndex.getEntryBytesListJson(routePar, 1))
                                }
                            }
                        }
                        post("/saveentry") {
                            val entryJson: EntryJson = call.receive()
                            val entry = decode(entryJson.entry) as Song
                            call.respond(
                                m1GlobalIndex.save(
                                    entry = entry,
                                    userName = call.principal<UserIdPrincipal>()!!.name
                                )
                            )
                        }
                    }
                    route("/m2") {
                        get("/indexselection") {
                            call.respond(m2GlobalIndex.getIndexUserSelection())
                        }
                        get("/entry/{searchString}") {
                            val routePar = call.parameters["searchString"]
                            if (routePar != null && routePar.isNotEmpty()) {
                                val queryPar = call.request.queryParameters["type"]
                                if (queryPar == "uid") {
                                    call.respond(m2GlobalIndex.getBytes(routePar.toInt()))
                                } else if (queryPar == "name") {
                                    call.respond(m2GlobalIndex.getEntryBytesListJson(routePar, 1))
                                }
                            }
                        }
                        post("/saveentry") {
                            val entryJson: EntryJson = call.receive()
                            val entry = decode(entryJson.entry) as Contact
                            call.respond(
                                m2GlobalIndex.save(
                                    entry = entry,
                                    userName = call.principal<UserIdPrincipal>()!!.name
                                )
                            )
                        }
                    }
                    route("/m3") {
                        get("/indexselection") {
                            call.respond(m3GlobalIndex.getIndexUserSelection())
                        }
                        get("/entry/{searchString}") {
                            val routePar = call.parameters["searchString"]
                            if (routePar != null && routePar.isNotEmpty()) {
                                val queryPar = call.request.queryParameters["type"]
                                if (queryPar == "uid") {
                                    call.respond(m3GlobalIndex.getBytes(routePar.toInt()))
                                } else if (queryPar == "name") {
                                    call.respond(m3GlobalIndex.getEntryBytesListJson(routePar, 1))
                                }
                            }
                        }
                        post("/saveentry") {
                            val entryJson: EntryJson = call.receive()
                            val entry = decode(entryJson.entry) as Invoice
                            call.respond(
                                m3GlobalIndex.save(
                                    entry = entry,
                                    userName = call.principal<UserIdPrincipal>()!!.name
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    init {
        runAsync {
            serverEngine.start(wait = true)
        }
    }
}