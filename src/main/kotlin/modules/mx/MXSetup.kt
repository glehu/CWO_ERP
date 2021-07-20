package modules.mx

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.misc.MXUser
import java.io.File

//Active user
var activeUser: MXUser = MXUser("","")

//Token for encryption/decryption
var token: String = ""

//File locations
val programPath: String = System.getProperty("user.dir")
var dataPath: String = ""
fun getModulePath(module: String) = "$dataPath\\data\\$module"

//Search settings
var maxSearchResultsGlobal: Int = 0

fun checkIniFile(iniFile: File)
{
    if (!iniFile.isFile)
    {
        iniFile.createNewFile()
        //Now we have to initialize it
        iniFile.writeText(Json.encodeToString(IniValues(
            token = "8265726400192847",
            dataPath = System.getProperty("user.dir"),
            maxSearchResults = 10_000
        )))
    }
    val iniValuesLoad = Json.decodeFromString<IniValues>(iniFile.readText().replace("\\", "\\\\"))
    dataPath = iniValuesLoad.dataPath
    token = iniValuesLoad.token
    maxSearchResultsGlobal = iniValuesLoad.maxSearchResults
}

@Serializable
private data class IniValues(
    @SerialName("encryption key") var token: String,
    @SerialName("data path") var dataPath: String,
    @SerialName("max search results") var maxSearchResults: Int)