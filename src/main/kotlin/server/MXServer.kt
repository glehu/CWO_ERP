package server

import modules.api.logic.SpotifyAPI
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import modules.api.gui.MGXSpotify
import modules.mx.logic.MXLog
import tornadofx.find
import tornadofx.runAsync

class MXServer
{
    val serverEngine = embeddedServer(Netty, port = 8000) {
        routing {
            get("/") {
                MXLog.log("MX", MXLog.LogType.COM, "User Connected", "server")
                //Reaction
                call.respondText("CWO ERP Embedded Server")
            }
            get("/authcallback/spotify") {
                MXLog.log("MX", MXLog.LogType.COM, "Spotify Auth Callback received", "server")
                call.respondText("CWO ERP Spotify Authorization Callback Site")
                val code: String? = call.request.queryParameters["code"]
                find<MGXSpotify>().authCodeProperty.value = code
                find<MGXSpotify>().showTokenData(SpotifyAPI().getAccessTokenFromAuthCode(code!!))
            }
        }
    }
    init
    {
        runAsync {
            serverEngine.start(wait = true)
        }
    }
}