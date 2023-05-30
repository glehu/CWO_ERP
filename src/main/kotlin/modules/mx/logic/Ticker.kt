package modules.mx.logic

import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m5.logic.tickerDistributeMessageBadges

/**
 * The Ticker class for the client solution.
 *
 * Every set amount of seconds, an operation is executed, to retrieve necessary data from the server.
 * Other useful functions that need to be executed regularly can be put here, too.
 */
class Ticker {
  @ExperimentalCoroutinesApi
  @DelicateCoroutinesApi
  @ExperimentalSerializationApi
  @InternalAPI
  companion object TickerTask {
    fun startTicker() = GlobalScope.launch {
      val delayMS = (10L * 60_000L)
      println("[ TICKER INIT ]")
      do {
        println("[ TICKER WAIT $delayMS ms ]")
        delay(delayMS)
        println("[ TICKER PROC ]")
        // #### General Ticker Actions: ####
        tickerDistributeMessageBadges()
      } while (true)
    }
  }
}
