package modules.mx

import api.logic.MXServer
import interfaces.IEntry
import io.ktor.util.*
import javafx.scene.image.Image
import kotlinx.coroutines.Job
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import modules.m1.M1Song
import modules.m1.logic.M1IndexManager
import modules.m2.M2Contact
import modules.m2.logic.M2IndexManager
import modules.m3.M3Invoice
import modules.m3.M3InvoicePosition
import modules.m3.logic.M3IndexManager
import modules.m4.M4Item
import modules.m4.logic.M4IndexManager
import java.io.File

//*************************************************
//********************** SERIALIZERS **+***********
//*************************************************

@ExperimentalSerializationApi
val serializersModuleGlobal = SerializersModule {
    polymorphic(IEntry::class) {
        subclass(M1Song::class, serializer())
        subclass(M2Contact::class, serializer())
        subclass(M3Invoice::class, serializer())
        subclass(M3InvoicePosition::class, serializer())
        subclass(M4Item::class, serializer())
    }
}

@ExperimentalSerializationApi
val protoBufGlobal = ProtoBuf {
    serializersModule = serializersModuleGlobal
}

//*************************************************
//********************** INDEX MANAGERS ***********
//*************************************************

/**
 * The global index for songs
 */
@ExperimentalSerializationApi
lateinit var m1GlobalIndex: M1IndexManager

/**
 * The global index for contacts
 */
@ExperimentalSerializationApi
lateinit var m2GlobalIndex: M2IndexManager

/**
 * The global index for invoices
 */
@ExperimentalSerializationApi
lateinit var m3GlobalIndex: M3IndexManager

/**
 * The global index for items
 */
@ExperimentalSerializationApi
lateinit var m4GlobalIndex: M4IndexManager

//*************************************************
//********************** MISCELLANEOUS ************
//*************************************************

//Instance title
var titleGlobal: String = "CWO ERP"

//Active user
var activeUser: MXUser = MXUser("", "")

//Token for encryption/decryption
var tokenGlobal: String = ""

//Task
lateinit var taskJobGlobal: Job

//File locations
val programPath: String = System.getProperty("user.dir")
var dataPath: String = ""
fun getModulePath(module: String) = "$dataPath\\data\\$module"
fun getIniFile() = File("$programPath\\cwo_erp.ini")
fun getClientSecretFile(api: String) = File("$dataPath\\data\\api\\${api}_cs.txt")
fun getIcon() = Image("file:///$dataPath\\data\\img\\orochi_logo_red_200x200.png")
fun getLogo() = Image("file:///$dataPath\\data\\img\\orochi_logo_red_500x500.png")

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