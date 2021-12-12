package modules.mx.logic

import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.gui.GItemPriceManager
import modules.m4.gui.GItemStorageManager
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
    companion object MXTask {
        fun startTicker() = GlobalScope.launch {
            do {
                delay(5000L)
                if (isClientGlobal) {
                    /**
                     * #### Client Ticker Actions: ####
                     */
                    find<GItemPriceManager>().refreshCategories()
                    find<GItemStorageManager>().refreshStorages()
                } else {
                    /**
                     * #### Server Ticker Actions: ####
                     */
                    find<GDatabaseManager>().refreshStats()
                }
                /**
                 * #### General Ticker Actions: ####
                 */
            } while (true)
        }
    }
}
