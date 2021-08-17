package modules.mx

import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.logic.M1IndexManager
import modules.m2.logic.M2IndexManager
import modules.m3.logic.M3IndexManager
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
fun getIniFile() = File("$programPath\\cwo_erp.ini")

//Search settings
var maxSearchResultsGlobal: Int = 0

//GUI settings
const val rightButtonsWidth = 150.0

//Time
var differenceFromUTC: Int = 0

//Server
lateinit var server: MXServer