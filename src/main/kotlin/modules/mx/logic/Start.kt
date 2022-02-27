package modules.mx.logic

import api.logic.UsageTracker
import api.logic.core.Server
import api.logic.core.TelnetServer
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.m1.logic.DiscographyIndexManager
import modules.m2.logic.ContactIndexManager
import modules.m3.logic.InvoiceIndexManager
import modules.m4.logic.ItemIndexManager
import modules.m4stockposting.logic.ItemStockPostingIndexManager
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
    // GUI
    launch<CWOMainGUI>()
  } else if (args[0] == "cli" || args[0] == "cmd") {
    // Command Line Interpreter
    CLI().runCLI()
  }
}

@InternalAPI
@ExperimentalSerializationApi
fun checkInstallation() {
  //Search for the .ini file to set up the software
  if (!getIniFile().isFile) {
    if (!cliMode) {
      showPreferences()
    } else {
      //TODO
    }
  } else readAndSetIniValues()
  if (!isClientGlobal) {
    //Check if all data paths and files exist
    checkModules()
    //Check if all log paths and files exist
    checkLogFiles()
  }
}

@ExperimentalSerializationApi
@InternalAPI
fun checkLogFiles() {
  Log.checkLogFile("MX", true)
  Log.checkLogFile("M1", true)
  Log.checkLogFile("M2", true)
  Log.checkLogFile("M3", true)
  Log.checkLogFile("M4", true)
  Log.checkLogFile("M4SP", true)
}

fun checkModules() {
  checkModuleDir("MX")
  checkModuleDir("M1")
  checkModuleDir("M2")
  checkModuleDir("M3")
  checkModuleDir("M4")
  checkModuleDir("M4SP")
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
fun startupRoutines(modeOffline: Boolean, modeSafety: Boolean) {
  if (!isClientGlobal) {
    //Load index managers
    if (!modeSafety) loadIndex()
    //Start the embedded server and usage tracker
    if (!modeOffline) {
      server = Server()
      telnetServer = TelnetServer()
      usageTracker = UsageTracker()
    }
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
    }
  } else {
    discographyIndexManager = DiscographyIndexManager()
    contactIndexManager = ContactIndexManager()
    invoiceIndexManager = InvoiceIndexManager()
    itemIndexManager = ItemIndexManager()
    itemStockPostingIndexManager = ItemStockPostingIndexManager()
  }
}

@DelicateCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
fun exitMain() {
  if (activeUser.username.isEmpty()) return
  UserManager().logout(activeUser.username, activeUser.password)
  if (!isClientGlobal) {
    if (serverJobGlobal != null && serverJobGlobal!!.isActive) {
      Log.log(Log.LogType.INFO, "Shutting down server...")
      try {
        server.serverEngine.stop(100L, 100L)
      } catch (_: IOException) {
      } finally {
        serverJobGlobal!!.cancel()
      }
    }
    if (telnetServerJobGlobal != null && telnetServerJobGlobal!!.isActive) {
      Log.log(Log.LogType.INFO, "Shutting down telnet server...")
      try {
        telnetServer.telnetServer.close()
      } catch (_: IOException) {
      } finally {
        telnetServerJobGlobal!!.cancel()
      }
    }
  }
  if (taskJobGlobal != null && taskJobGlobal!!.isActive) {
    Log.log(Log.LogType.INFO, "Shutting down ticker...")
    taskJobGlobal!!.cancel()
  }
}
