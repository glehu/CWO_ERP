package modules.mx

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

//Token for encryption/decryption
var token: String = ""

//File locations
val programPath: String = System.getProperty("user.dir")
var dataPath: String = ""
fun getModulePath(module: String) = "$dataPath\\data\\$module"

fun checkIniFile(iniFile: File)
{
    if (!iniFile.isFile)
    {
        iniFile.createNewFile()
        //Now we have to initialize it
        iniFile.writeText(Json.encodeToString(IniValues("8265726400192847", System.getProperty("user.dir"))))
    }
    val iniValuesLoad = Json.decodeFromString<IniValues>(iniFile.readText())
    dataPath = iniValuesLoad.dataPath
    token = iniValuesLoad.token
}

@Serializable
private data class IniValues(var token: String, var dataPath: String)