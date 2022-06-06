package api.logic.core

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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.mx.Ini
import modules.mx.getIniFile
import modules.mx.logic.Log
import modules.mx.telnetServerJobGlobal
import java.net.InetSocketAddress

@DelicateCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
class TelnetServer : IModule {
  override val moduleNameLong = "TelnetServer"
  override val module = "MX"
  override fun getIndexManager(): IIndexManager? {
    return null
  }

  private val iniVal = Json.decodeFromString<Ini>(getIniFile().readText())

  val telnetServer =
    aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(
      localAddress = InetSocketAddress(
        iniVal.telnetServerIPAddress.substringBefore(':'),
        iniVal.telnetServerIPAddress.substringAfter(':').toInt()
      )
    )

  init {
    log(
      type = Log.Type.SYS,
      text = "TELNET SERVE ${telnetServer.localAddress}"
    )
    telnetServerJobGlobal = GlobalScope.launch {
      while (true) {
        log(
          type = Log.Type.INFO,
          text = "TELNET READY"
        )
        val socket = telnetServer.accept()
        launch {
          log(
            type = Log.Type.COM,
            text = "TELNET OPEN ${socket.remoteAddress}",
            apiEndpoint = "TELNET ${socket.localAddress}"
          )
          RawSocketChannel(socket).startSession()
        }
      }
    }
  }
}
