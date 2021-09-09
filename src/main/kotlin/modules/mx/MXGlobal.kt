package modules.mx

import api.logic.MXServer
import interfaces.IEntry
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import modules.m1.Song
import modules.m1.logic.M1IndexManager
import modules.m2.Contact
import modules.m2.logic.M2IndexManager
import modules.m3.Invoice
import modules.m3.Item
import modules.m3.logic.M3IndexManager
import java.io.File

@ExperimentalSerializationApi
val serializersModuleGlobal = SerializersModule {
    polymorphic(IEntry::class) {
        subclass(Song::class, serializer())
        subclass(Contact::class, serializer())
        subclass(Invoice::class, serializer())
        subclass(Item::class, serializer())
    }
}

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

//Instance title
var titleGlobal: String = "CWO ERP"

//Active user
var activeUser: MXUser = MXUser("", "")

//Token for encryption/decryption
var token: String = ""

//File locations
val programPath: String = System.getProperty("user.dir")
var dataPath: String = ""
fun getModulePath(module: String) = "$dataPath\\data\\$module"
fun getIniFile() = File("$programPath\\cwo_erp.ini")
fun getClientSecretFile(api: String) = File("$dataPath\\data\\api\\${api}_cs.txt")

//Search settings
var maxSearchResultsGlobal: Int = 0

//GUI settings
const val rightButtonsWidth = 150.0

//Time
var differenceFromUTC: Int = 0

//Server/Client
@InternalAPI
@ExperimentalSerializationApi
lateinit var server: MXServer
var isClientGlobal: Boolean = false
var serverIPAddressGlobal: String = "?"