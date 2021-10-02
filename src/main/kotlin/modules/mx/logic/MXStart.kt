package modules.mx.logic

import api.logic.MXServer
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.m1.logic.M1IndexManager
import modules.m2.logic.M2IndexManager
import modules.m3.logic.M3IndexManager
import modules.m4.logic.M4IndexManager
import modules.mx.*
import modules.mx.gui.CWOMainGUI
import modules.mx.gui.showPreferences
import tornadofx.launch
import java.io.File

@DelicateCoroutinesApi
@InternalAPI
@ExperimentalSerializationApi
fun main() {
    launch<CWOMainGUI>()
}

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
        //Check if all log paths and files exist
        MXLog.checkLogFile("MX", true)
        MXLog.checkLogFile("M1", true)
        MXLog.checkLogFile("M2", true)
        MXLog.checkLogFile("M3", true)
        MXLog.checkLogFile("M4", true)
    }
}

@ExperimentalSerializationApi
fun readAndSetIniValues() {
    val iniVal = Json.decodeFromString<MXIni>(getIniFile().readText())
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
        m1GlobalIndex = M1IndexManager()
        m2GlobalIndex = M2IndexManager()
        m3GlobalIndex = M3IndexManager()
        m4GlobalIndex = M4IndexManager()
        /**
         * Start the embedded server
         */
        server = MXServer()
    }
    /**
     * Start a long-running coroutine task to do various stuff
     */
    taskJobGlobal = MXTicker.startTicker()
}