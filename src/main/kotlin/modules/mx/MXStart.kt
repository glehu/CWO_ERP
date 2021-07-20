package modules.mx

import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.gui.CWOMainGUI
import modules.mx.logic.MXLog
import modules.mx.misc.MXUser
import tornadofx.launch
import java.io.File

@ExperimentalSerializationApi
fun main()
{
    launch<CWOMainGUI>()
}

fun loginRoutines()
{
    //Search for the .ini file to setup the software
    checkIniFile(File("$programPath\\cwo_erp.ini"))
}

fun startupRoutines(user: MXUser)
{
    checkIniFile(File("$programPath\\cwo_erp.ini"))
    //Set active user
    activeUser = user
    //Check if all data paths and files exist
    if (!File(getModulePath("M1")).isDirectory) File(getModulePath("M1")).mkdirs()
    if (!File(getModulePath("M2")).isDirectory) File(getModulePath("M2")).mkdirs()
    if (!File(getModulePath("MX")).isDirectory) File(getModulePath("MX")).mkdirs()
    //Check if all log paths and files exist
    MXLog.checkLogFile("M1", true)
    MXLog.checkLogFile("M2", true)
    MXLog.checkLogFile("MX", true)
}