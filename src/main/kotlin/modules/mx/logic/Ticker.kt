package modules.mx.logic

import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.gui.GDatabaseManager
import modules.mx.isClientGlobal
import tornadofx.find

/**
 * The Ticker class for the client solution.
 *
 * Every set amount of seconds, an operation is executed, to retrieve necessary data from the server.
 * Other useful functions that need to be executed regularly can be put here, too.
 */
class Ticker {
  @DelicateCoroutinesApi
  @ExperimentalSerializationApi
  @InternalAPI
  companion object TickerTask {
    fun startTicker() = GlobalScope.launch {
      do {
        delay(5000L)
        if (isClientGlobal) {
          // #### Client Ticker Actions: ####
        } else {
          // #### Server Ticker Actions: ####
          find<GDatabaseManager>().refreshStats()
        }
        // #### General Ticker Actions: ####
      } while (true)
    }
  }
}
