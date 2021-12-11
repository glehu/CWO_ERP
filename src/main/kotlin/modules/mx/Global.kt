package modules.mx

import api.logic.Server
import api.logic.UsageTracker
import interfaces.IEntry
import io.ktor.util.*
import javafx.scene.image.Image
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import modules.m1.Song
import modules.m1.logic.DiscographyIndexManager
import modules.m2.Contact
import modules.m2.logic.ContactIndexManager
import modules.m3.Invoice
import modules.m3.InvoicePosition
import modules.m3.logic.InvoiceIndexManager
import modules.m4.Item
import modules.m4.ItemStockPosting
import modules.m4.logic.ItemIndexManager
import modules.m4.logic.ItemStockPostingIndexManager
import java.io.File

//*************************************************
//********************** SERIALIZERS **+***********
//*************************************************

@InternalAPI
@ExperimentalSerializationApi
val serializersModuleGlobal = SerializersModule {
    polymorphic(IEntry::class) {
        subclass(Song::class, serializer())
        subclass(Contact::class, serializer())
        subclass(Invoice::class, serializer())
        subclass(InvoicePosition::class, serializer())
        subclass(Item::class, serializer())
        subclass(ItemStockPosting::class, serializer())
    }
}

@InternalAPI
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
@InternalAPI
@ExperimentalSerializationApi
var m1GlobalIndex: DiscographyIndexManager? = null

/**
 * The global index for contacts
 */
@InternalAPI
@ExperimentalSerializationApi
var m2GlobalIndex: ContactIndexManager? = null

/**
 * The global index for invoices
 */
@InternalAPI
@ExperimentalSerializationApi
var m3GlobalIndex: InvoiceIndexManager? = null

/**
 * The global index for items
 */
@InternalAPI
@ExperimentalSerializationApi
var m4GlobalIndex: ItemIndexManager? = null

/**
 * The global index for storage posting
 */
@InternalAPI
@ExperimentalSerializationApi
var m4StockPostingGlobalIndex: ItemStockPostingIndexManager? = null

//*************************************************
//********************** TRACKER ******************
//*************************************************

@InternalAPI
@ExperimentalSerializationApi
lateinit var usageTracker: UsageTracker

//*************************************************
//********************** MISCELLANEOUS ************
//*************************************************

//Instance title
var titleGlobal: String = "CWO ERP"

//Active user
var activeUser: User = User("", "")

//Token for encryption/decryption
var tokenGlobal: String = ""

//Task
var taskJobGlobal: Job? = null

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
@DelicateCoroutinesApi
@InternalAPI
@ExperimentalSerializationApi
lateinit var server: Server
var serverJobGlobal: Job? = null
var isClientGlobal: Boolean = false
var serverIPAddressGlobal: String = "?"

/**
 * CLI mode
 */
var cliMode = false
