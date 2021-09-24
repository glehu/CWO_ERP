package modules.mx.logic

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class MXTicker {
    companion object MXTask {
        fun startTicker() = runBlocking {
            launch {
                do {
                    delay(5000L)
                    /**
                     * #### Ticker Actions: ####
                     */


                    /**
                     * #########################
                     */
                } while (true)
            }
        }
    }
}