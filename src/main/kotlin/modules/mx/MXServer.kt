package modules.mx

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import modules.mx.logic.MXLog
import tornadofx.runAsync

class MXServer
{
    init
    {
        runAsync {
            embeddedServer(Netty, port = 8000) {
                routing {
                    get("/") {
                        MXLog.log("MX", MXLog.LogType.INFO, "User Connected", "server")
                        //Reaction
                        call.respondText("CWO ERP Embedded Server")
                    }
                }
            }.start(wait = true)
        }
    }
}