package server

import interfaces.IModule
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.api.gui.GSpotify
import modules.api.json.SpotifyAuthCallbackJson
import modules.api.logic.SpotifyAUTH
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
                                val entry = M1Controller().getEntry(routePar.toInt())
                                call.respond(Json.encodeToString(entry))
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