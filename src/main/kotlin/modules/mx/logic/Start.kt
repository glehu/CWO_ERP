package modules.mx.logic

import api.logic.UsageTracker
import api.logic.core.Server
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.m1.logic.DiscographyIndexManager
import modules.m2.logic.ContactIndexManager
import modules.m3.logic.InvoiceIndexManager
import modules.m4.logic.ItemIndexManager
import modules.m4stockposting.logic.ItemStockPostingIndexManager
import modules.m5.logic.UniChatroomIndexManager
import modules.m5messages.logic.UniMessagesIndexManager
import modules.m6.logic.SnippetBaseIndexManager
import modules.mx.Ini
import modules.mx.contactIndexManager
import modules.mx.dataPath
import modules.mx.differenceFromUTC
import modules.mx.discographyIndexManager
import modules.mx.getIniFile
import modules.mx.getModulePath
import modules.mx.invoiceIndexManager
import modules.mx.itemIndexManager
import modules.mx.itemStockPostingIndexManager
import modules.mx.maxSearchResultsGlobal
import modules.mx.server
import modules.mx.serverIPAddressGlobal
import modules.mx.serverJobGlobal
import modules.mx.snippetBaseIndexManager
import modules.mx.taskJobGlobal
import modules.mx.titleGlobal
import modules.mx.tokenGlobal
import modules.mx.uniChatroomIndexManager
import modules.mx.uniMessagesIndexManager
import modules.mx.usageTracker
import java.io.File
import java.io.IOException

@ExperimentalSerializationApi
@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@InternalAPI
suspend fun main(args: Array<String>) {
  if (args.isEmpty() || args[0] == "-gui") {
    return
  } else if (args[0] == "-cli" || args[0] == "-cmd") {
    // Command Line Interpreter
    CLI().runCLI(args)
  }
}

@InternalAPI
@ExperimentalSerializationApi
suspend fun checkInstallation() {
  //Search for the .ini file to set up the software
  if (!getIniFile().isFile) {
    //TODO
  } else readAndSetIniValues()
  //Check if all data paths and files exist
  checkModules()
  //Check if all log paths and files exist
  checkLogFiles()
}

@ExperimentalSerializationApi
@InternalAPI
suspend fun checkLogFiles() {
  Log.checkLogFile("MX", true)
  Log.checkLogFile("M1", true)
  Log.checkLogFile("M2", true)
  Log.checkLogFile("M3", true)
  Log.checkLogFile("M4", true)
  Log.checkLogFile("M4SP", true)
  Log.checkLogFile("M5", true)
  Log.checkLogFile("M5MSG", true)
  Log.checkLogFile("M6", true)
}

fun checkModules() {
  checkModuleDir("MX")
  checkModuleDir("M1")
  checkModuleDir("M2")
  checkModuleDir("M3")
  checkModuleDir("M4")
  checkModuleDir("M4SP")
  checkModuleDir("M5")
  checkModuleDir("M5MSG")
  checkModuleDir("M6")
}

fun checkModuleDir(module: String) {
  if (module.isEmpty()) return
  if (!File(getModulePath(module)).isDirectory) File(getModulePath(module)).mkdirs()
}

@ExperimentalSerializationApi
fun readAndSetIniValues() {
  val iniVal = Json.decodeFromString<Ini>(getIniFile().readText())
  tokenGlobal = iniVal.token
  dataPath = iniVal.dataPath
  maxSearchResultsGlobal = iniVal.maxSearchResults
  differenceFromUTC = iniVal.differenceFromUTC
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
@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@InternalAPI
@ExperimentalSerializationApi
fun startupRoutines(modeOffline: Boolean, modeSafety: Boolean) {
  //Load index managers
  if (!modeSafety) loadIndex()
  //Start the embedded server and usage tracker
  if (!modeOffline) {
    server = Server()
    usageTracker = UsageTracker()
  }
  // Start a long-running coroutine task to do various stuff
  taskJobGlobal = Ticker.startTicker()
}

@InternalAPI
@ExperimentalSerializationApi
fun loadIndex(module: String = "") {
  if (module.isNotEmpty()) {
    when (module) {
      "m1" -> discographyIndexManager = DiscographyIndexManager()
      "m2" -> contactIndexManager = ContactIndexManager()
      "m3" -> invoiceIndexManager = InvoiceIndexManager()
      "m4" -> itemIndexManager = ItemIndexManager()
      "m4sp" -> itemStockPostingIndexManager = ItemStockPostingIndexManager()
      "m5" -> uniChatroomIndexManager = UniChatroomIndexManager()
      "m5msg" -> uniMessagesIndexManager = UniMessagesIndexManager()
      "m6" -> snippetBaseIndexManager = SnippetBaseIndexManager()
    }
  } else {
    discographyIndexManager = DiscographyIndexManager()
    contactIndexManager = ContactIndexManager()
    invoiceIndexManager = InvoiceIndexManager()
    itemIndexManager = ItemIndexManager()
    itemStockPostingIndexManager = ItemStockPostingIndexManager()
    uniChatroomIndexManager = UniChatroomIndexManager()
    uniMessagesIndexManager = UniMessagesIndexManager()
    snippetBaseIndexManager = SnippetBaseIndexManager()
  }
}

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
suspend fun exitMain() {
  if (serverJobGlobal != null && serverJobGlobal!!.isActive) {
    Log.log(Log.Type.SYS, "Shutting down server...")
    try {
      server.serverEngine.stop(100L, 100L)
    } catch (_: IOException) {
    } finally {
      serverJobGlobal!!.cancel()
    }
  }
  if (taskJobGlobal != null && taskJobGlobal!!.isActive) {
    Log.log(Log.Type.SYS, "Shutting down ticker...")
    taskJobGlobal!!.cancel()
  }
}
