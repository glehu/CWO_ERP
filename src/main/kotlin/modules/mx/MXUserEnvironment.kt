package modules.mx

import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.logic.MXLog
import tornadofx.launch
import java.io.File

@ExperimentalSerializationApi
fun main() { launch<CWOMainGUI>() }

fun getToken() = "8265726400192847"
fun getProgramPath() = "C:\\ProgramData\\Orochi\\cwo"
fun getModulePath(module: String) = "${getProgramPath()}\\data\\$module"
fun startupRoutines()
{
    if (!File(getModulePath("M1")).isDirectory) { File(getModulePath("M1")).mkdirs() }
    if (!File(getModulePath("M2")).isDirectory) { File(getModulePath("M2")).mkdirs() }
    MXLog.checkLogFile("M1", true)
    MXLog.checkLogFile("M2", true)
}