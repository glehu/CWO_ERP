package api.logic

import api.gui.GSpotify
import api.misc.json.M1EntryJson
import api.misc.json.M2EntryJson
import api.misc.json.M3EntryJson
import api.misc.json.SpotifyAuthCallbackJson
import db.CwODB
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
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import modules.m1.Song
import modules.m1.logic.M1Controller
import modules.m1.logic.M1DBManager
import modules.m2.Contact
import modules.m2.logic.M2Controller
import modules.m2.logic.M2DBManager
import modules.m3.Invoice
import modules.m3.logic.M3Controller
import modules.m3.logic.M3DBManager
import modules.mx.logic.MXLog
import modules.mx.logic.MXUserManager
import modules.mx.m1GlobalIndex
import modules.mx.m2GlobalIndex
import modules.mx.m3GlobalIndex
import tornadofx.Controller

@InternalAPI
@ExperimentalSerializationApi
class MXServer : IModule, Controller() {
    override fun moduleNameLong() = "Server"
    override fun module() = "MX"

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
                    call.respond(true)
                }
            }
            get("/authcallback/spotify") {
                MXLog.log(module(), MXLog.LogType.COM, "Spotify Auth Callback received", moduleNameLong())
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
                    MXLog.log(
                        module(),
                        MXLog.LogType.COM,
                        "User ${call.principal<UserIdPrincipal>()?.name} Connected",
                        moduleNameLong()
                    )
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
                                    call.respond(M1Controller().getEntryBytes(routePar.toInt()))
                                } else if (queryPar == "name") {
                                    call.respond(M1Controller().getEntryBytesListJson(routePar, 1))
                                }
                            }
                        }
                        post("/saveentry") {
                            val entryJson: M1EntryJson = call.receive()
                            val entry = ProtoBuf.decodeFromByteArray<Song>(entryJson.entry)
                            val raf = CwODB().openRandomFileAccess("M1", CwODB.RafMode.READWRITE)
                            call.respond(
                                M1DBManager().saveEntry(
                                    entry = entry,
                                    cwodb = CwODB(),
                                    posDB = -1L,
                                    byteSize = -1,
                                    raf = raf,
                                    indexManager = m1GlobalIndex
                                )
                            )
                            CwODB().closeRandomFileAccess(raf)
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
                                    call.respond(M2Controller().getEntryBytes(routePar.toInt()))
                                } else if (queryPar == "name") {
                                    call.respond(M2Controller().getEntryBytesListJson(routePar, 1))
                                }
                            }
                        }
                        post("/saveentry") {
                            val entryJson: M2EntryJson = call.receive()
                            val entry = ProtoBuf.decodeFromByteArray<Contact>(entryJson.entry)
                            val raf = CwODB().openRandomFileAccess("M2", CwODB.RafMode.READWRITE)
                            call.respond(
                                M2DBManager().saveEntry(
                                    entry = entry,
                                    cwodb = CwODB(),
                                    posDB = -1L,
                                    byteSize = -1,
                                    raf = raf,
                                    indexManager = m2GlobalIndex
                                )
                            )
                            CwODB().closeRandomFileAccess(raf)
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
                                    call.respond(M3Controller().getEntryBytes(routePar.toInt()))
                                } else if (queryPar == "name") {
                                    call.respond(M3Controller().getEntryBytesListJson(routePar, 1))
                                }
                            }
                        }
                        post("/saveentry") {
                            val entryJson: M3EntryJson = call.receive()
                            val entry = ProtoBuf.decodeFromByteArray<Invoice>(entryJson.entry)
                            val raf = CwODB().openRandomFileAccess("M3", CwODB.RafMode.READWRITE)
                            call.respond(
                                M3DBManager().saveEntry(
                                    entry = entry,
                                    cwodb = CwODB(),
                                    posDB = -1L,
                                    byteSize = -1,
                                    raf = raf,
                                    indexManager = m3GlobalIndex
                                )
                            )
                            CwODB().closeRandomFileAccess(raf)
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