package modules.mx.logic

import io.ktor.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.gui.MG4PriceManager
import tornadofx.find


class MXTicker {
    @ExperimentalSerializationApi
    @InternalAPI
    companion object MXTask {
        fun startTicker() = runBlocking {
            launch {
                do {
                    delay(5000L)
                    /**
                     * #### Ticker Actions: ####
                     */

                    find<MG4PriceManager>().refreshCategories()

                    /**
                     * #########################
                     */
                } while (true)
            }
        }
    }
}