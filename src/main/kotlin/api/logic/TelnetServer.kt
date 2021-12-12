package api.logic

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.logic.Log
import modules.mx.telnetServerJobGlobal
import java.net.InetSocketAddress


@DelicateCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
class TelnetServer : IModule {
    override val moduleNameLong = "Server"
    override val module = "MX"
    override fun getIndexManager(): IIndexManager? {
        return null
    }

    val server =
        aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
            .bind(InetSocketAddress("127.0.0.1", 2323))

    init {
        log(
            logType = Log.LogType.SYS,
            text = "TELNET OPEN ${server.localAddress}"
        )
        telnetServerJobGlobal = GlobalScope.launch {
            while (true) {
                val socket = server.accept()
                launch {
                    RawSocketChannel(socket).startSession()
                }
            }
        }
    }
}
