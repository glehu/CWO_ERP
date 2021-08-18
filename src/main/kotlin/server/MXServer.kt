package server

import api.SpotifyAPI
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import modules.mx.gui.api.MGXSpotify
import modules.mx.logic.MXLog
import tornadofx.find
import tornadofx.runAsync

class MXServer
{
    lateinit var authHTML: String

    init
    {
        runAsync {
            embeddedServer(Netty, port = 8000) {
                install(ContentNegotiation) {
                    json()
                }
                routing {
                    get("/") {
                        MXLog.log("MX", MXLog.LogType.COM, "User Connected", "server")
                        //Reaction
                        call.respondText("CWO ERP Embedded Server")
                    }
                    get("/authcallback/spotify") {
                        MXLog.log("MX", MXLog.LogType.COM, "Spotify Auth Callback received", "server")
                        call.respondText("CWO ERP Authorization Callback Site")
                        val code: String? = call.request.queryParameters["code"]
                        find<MGXSpotify>().authCodeProperty.value = code
                        find<MGXSpotify>().responseProperty.value =
                            SpotifyAPI().getAccessTokenFromAuthCode(code!!)
                    }
                }
            }.start(wait = true)
        }
    }
}