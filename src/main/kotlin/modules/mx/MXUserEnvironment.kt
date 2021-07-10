package modules.mx

import kotlinx.serialization.ExperimentalSerializationApi
import tornadofx.launch
import java.io.File

@ExperimentalSerializationApi
fun main() { launch<CWOMainGUI>() }

fun getModulePath(module: String) = "C:\\ProgramData\\Orochi\\cwo\\data\\$module"
fun startupRoutines()
{
    if (!File(getModulePath("M1")).isDirectory) { File(getModulePath("M1")).mkdirs() }
    if (!File(getModulePath("M2")).isDirectory) { File(getModulePath("M2")).mkdirs() }
    MXLog.checkLogFile("M1", true)
    MXLog.checkLogFile("M2", true)
}