package modules.mx.logic

import api.logic.Server
import api.logic.TelnetServer
import api.logic.UsageTracker
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.m1.logic.DiscographyIndexManager
import modules.m2.logic.ContactIndexManager
import modules.m3.logic.InvoiceIndexManager
import modules.m4.logic.ItemIndexManager
import modules.m4.logic.ItemStockPostingIndexManager
import modules.mx.*
import modules.mx.gui.CWOMainGUI
import modules.mx.gui.showPreferences
import tornadofx.launch
import java.io.File
import java.io.IOException

@DelicateCoroutinesApi
@InternalAPI
@ExperimentalSerializationApi
fun main(args: Array<String>) {
    if (args.isEmpty() || args[0] == "gui") {
        /**
         * GUI
         */
        launch<CWOMainGUI>()
    } else if (args[0] == "cli" || args[0] == "cmd") {
        /**
         * Command Line Interpreter
         */
        cliMode = true
        CLI().runCLI()
    }
}

@InternalAPI
@ExperimentalSerializationApi
fun checkInstallation() {
    //Search for the .ini file to set up the software
    if (!getIniFile().isFile) {
        showPreferences()
    } else readAndSetIniValues()
    if (!isClientGlobal) {
        //Check if all data paths and files exist
        if (!File(getModulePath("MX")).isDirectory) File(getModulePath("MX")).mkdirs()
        if (!File(getModulePath("M1")).isDirectory) File(getModulePath("M1")).mkdirs()
        if (!File(getModulePath("M2")).isDirectory) File(getModulePath("M2")).mkdirs()
        if (!File(getModulePath("M3")).isDirectory) File(getModulePath("M3")).mkdirs()
        if (!File(getModulePath("M4")).isDirectory) File(getModulePath("M4")).mkdirs()
        if (!File(getModulePath("M4SP")).isDirectory) File(getModulePath("M4SP")).mkdirs()
        //Check if all log paths and files exist
        Log.checkLogFile("MX", true)
        Log.checkLogFile("M1", true)
        Log.checkLogFile("M2", true)
        Log.checkLogFile("M3", true)
        Log.checkLogFile("M4", true)
        Log.checkLogFile("M4SP", true)
    }
}

@ExperimentalSerializationApi
fun readAndSetIniValues() {
    val iniVal = Json.decodeFromString<Ini>(getIniFile().readText())
    tokenGlobal = iniVal.token
    dataPath = iniVal.dataPath
    maxSearchResultsGlobal = iniVal.maxSearchResults
    differenceFromUTC = iniVal.differenceFromUTC
    isClientGlobal = iniVal.isClient
    serverIPAddressGlobal = iniVal.serverIPAddress
    //Customize title
    titleGlobal += when (iniVal.isClient) {
        true -> " Client"
        false -> " Server"
    }
}

/**
 * Starts the software with all necessary precautions
 */
@DelicateCoroutinesApi
@InternalAPI
@ExperimentalSerializationApi
fun startupRoutines() {
    if (!isClientGlobal) {
        /**
         * Load index managers
         */
        loadIndex()
        /**
         * Start the embedded server and usage tracker
         */
        server = Server()
        telnetServer = TelnetServer()
        usageTracker = UsageTracker()
    }
    /**
     * Start a long-running coroutine task to do various stuff
     */
    taskJobGlobal = Ticker.startTicker()
}

@InternalAPI
@ExperimentalSerializationApi
fun loadIndex(module: String = "") {
    if (module.isNotEmpty()) {
        when (module) {
            "m1" -> m1GlobalIndex = DiscographyIndexManager()
            "m2" -> m2GlobalIndex = ContactIndexManager()
            "m3" -> m3GlobalIndex = InvoiceIndexManager()
            "m4" -> m4GlobalIndex = ItemIndexManager()
            "m4sp" -> m4StockPostingGlobalIndex = ItemStockPostingIndexManager()
        }
    } else {
        m1GlobalIndex = DiscographyIndexManager()
        m2GlobalIndex = ContactIndexManager()
        m3GlobalIndex = InvoiceIndexManager()
        m4GlobalIndex = ItemIndexManager()
        m4StockPostingGlobalIndex = ItemStockPostingIndexManager()
    }
}

@DelicateCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
fun exitMain() {
    UserManager().logout(activeUser.username, activeUser.password)
    if (!isClientGlobal) {
        if (serverJobGlobal != null && serverJobGlobal!!.isActive) {
            Log.log(Log.LogType.INFO, "Shutting down server...")
            server.serverEngine.stop(100L, 100L)
            serverJobGlobal!!.cancel()
        }
        if (telnetServerJobGlobal != null && telnetServerJobGlobal!!.isActive) {
            Log.log(Log.LogType.INFO, "Shutting down telnet server...")
            try {
                telnetServer.server.close()
            } catch (e: IOException) {
                println(e.message)
            }
            telnetServerJobGlobal!!.cancel()
        }
    }
    if (taskJobGlobal != null && taskJobGlobal!!.isActive) {
        Log.log(Log.LogType.INFO, "Shutting down ticker...")
        taskJobGlobal!!.cancel()
    }
}
