package server

import modules.api.logic.SpotifyAUTH
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import modules.api.gui.GSpotify
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
                find<GSpotify>().authCodeProperty.value = code
                find<GSpotify>().showTokenData(SpotifyAUTH().getAccessTokenFromAuthCode(code!!))
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