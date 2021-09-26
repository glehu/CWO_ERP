package modules.mx.logic

import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.gui.MG4PriceManager
import tornadofx.find


class MXTicker {
    @DelicateCoroutinesApi
    @ExperimentalSerializationApi
    @InternalAPI
    companion object MXTask {
        fun startTicker() = GlobalScope.launch {
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