package modules.mx

import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.logic.M1IndexManager
import modules.m2.logic.M2IndexManager
import modules.m3.logic.M3IndexManager
import modules.mx.gui.CWOMainGUI
import modules.mx.logic.*
import tornadofx.launch
import java.io.File

@ExperimentalSerializationApi
fun main()
{
    launch<CWOMainGUI>()
}

fun loginRoutines()
{
    //Search for the .ini file to set up the software
    checkIniFile()
}

@ExperimentalSerializationApi
fun startupRoutines(user: MXUser)
{
    //Set active user
    activeUser = user
    //Check if all data paths and files exist
    if (!File(getModulePath("MX")).isDirectory) File(getModulePath("MX")).mkdirs()
    if (!File(getModulePath("M1")).isDirectory) File(getModulePath("M1")).mkdirs()
    if (!File(getModulePath("M2")).isDirectory) File(getModulePath("M2")).mkdirs()
    if (!File(getModulePath("M3")).isDirectory) File(getModulePath("M3")).mkdirs()
    //Check if all log paths and files exist
    MXLog.checkLogFile("MX", true)
    MXLog.checkLogFile("M1", true)
    MXLog.checkLogFile("M2", true)
    MXLog.checkLogFile("M3", true)
    //Load IndexManagers
    m1GlobalIndex = M1IndexManager()
    m2GlobalIndex = M2IndexManager()
    m3GlobalIndex = M3IndexManager()
}