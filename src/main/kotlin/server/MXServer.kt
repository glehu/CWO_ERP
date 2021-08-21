package server

import interfaces.IModule
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.api.gui.GSpotify
import modules.api.json.SpotifyAuthCallbackJson
import modules.api.logic.SpotifyAUTH
import modules.mx.logic.MXLog
import tornadofx.find
import tornadofx.runAsync

@ExperimentalSerializationApi
class MXServer: IModule
{
    override fun moduleNameLong() = "Server"
    override fun module() = "MX"

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
        }
    }

    init
    {
        runAsync {
            serverEngine.start(wait = true)
        }
    }
}