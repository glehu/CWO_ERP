package modules.mx

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import modules.m1.logic.M1IndexManager
import modules.m2.logic.M2IndexManager
import modules.m3.logic.M3IndexManager
import modules.mx.logic.getRandomString
import java.io.File

//*************************************************
//********************** INDEX MANAGERS ***********
//*************************************************

@ExperimentalSerializationApi
lateinit var m1GlobalIndex: M1IndexManager
@ExperimentalSerializationApi
lateinit var m2GlobalIndex: M2IndexManager
@ExperimentalSerializationApi
lateinit var m3GlobalIndex: M3IndexManager

//*************************************************
//********************** MISCELLANEOUS ************
//*************************************************

//Active user
var activeUser: MXUser = MXUser("", "")

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
        iniFile.writeText(
            Json.encodeToString(
                IniValues(
                    token = getRandomString(16, true),
                    dataPath = System.getProperty("user.dir"),
                    maxSearchResults = 10_000
                )
            )
        )
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
    @SerialName("max search results") var maxSearchResults: Int
)