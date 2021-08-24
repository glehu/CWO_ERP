package api.logic

import api.gui.GSpotify
import api.misc.json.SpotifyAuthCallbackJson
import interfaces.IModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m1.logic.M1Controller
import modules.mx.logic.MXLog
import tornadofx.find
import tornadofx.runAsync

@ExperimentalSerializationApi
class MXServer : IModule {
    override fun moduleNameLong() = "Server"
    override fun module() = "MX"

    lateinit var text: String

    val serverEngine = embeddedServer(Netty, port = 8000) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
        routing {
            get("/") {
                MXLog.log(module(), MXLog.LogType.COM, "User Connected", moduleNameLong())
                //Reaction
                call.respondText("CWO ERP Embedded Server")
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
            route("/api")
            {
                route("/m1") {
                    get("") {
                        text = "/entry/{searchString}?type={type}" +
                                "\n\ntype can be \"uid\" if the search string is a unique identifier code"
                        call.respondText(text)
                    }
                    get("/entry/{searchString}") {
                        val routePar = call.parameters["searchString"]
                        if (routePar != null && routePar.isNotEmpty()) {
                            val queryPar = call.request.queryParameters["type"]
                            if (queryPar == "uid") {
                                call.respond(Json.encodeToString(M1Controller().getEntry(routePar.toInt())))
                            } else if (queryPar == "name") {
                                call.respond(M1Controller().getEntryListJson(routePar, 1))
                            }
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