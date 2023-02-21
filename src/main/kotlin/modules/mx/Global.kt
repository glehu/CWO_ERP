package modules.mx

import api.logic.UsageTracker
import api.logic.core.Server
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import interfaces.IEntry
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import modules.m4.logic.ItemIndexManager
import modules.m4stockposting.ItemStockPosting
import modules.m4stockposting.logic.ItemStockPostingIndexManager
import modules.m5.IUniRole
import modules.m5.UniChatroom
import modules.m5.UniRole
import modules.m5.logic.UniChatroomIndexManager
import modules.m5messages.UniMessage
import modules.m5messages.logic.UniMessagesIndexManager
import modules.m6.Snippet
import modules.m6.logic.SnippetBaseIndexManager
import modules.m7knowledge.Knowledge
import modules.m7knowledge.logic.KnowledgeIndexManager
import modules.m7wisdom.Wisdom
import modules.m7wisdom.logic.WisdomIndexManager
import modules.m8notification.Notification
import modules.m8notification.logic.NotificationIndexManager
import modules.m9process.ProcessEvent
import modules.m9process.logic.ProcessIndexManager
import java.io.File
import java.nio.file.Paths

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
    subclass(UniChatroom::class, serializer())
    subclass(UniMessage::class, serializer())
    subclass(Snippet::class, serializer())
    subclass(Knowledge::class, serializer())
    subclass(Wisdom::class, serializer())
    subclass(Notification::class, serializer())
    subclass(ProcessEvent::class, serializer())
  }
  polymorphic(IUniRole::class) {
    subclass(UniRole::class, serializer())
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
var discographyIndexManager: DiscographyIndexManager? = null

/**
 * The global index for contacts
 */
@InternalAPI
@ExperimentalSerializationApi
var contactIndexManager: ContactIndexManager? = null

/**
 * The global index for invoices
 */
@InternalAPI
@ExperimentalSerializationApi
var invoiceIndexManager: InvoiceIndexManager? = null

/**
 * The global index for items
 */
@InternalAPI
@ExperimentalSerializationApi
var itemIndexManager: ItemIndexManager? = null

/**
 * The global index for storage posting
 */
@InternalAPI
@ExperimentalSerializationApi
var itemStockPostingIndexManager: ItemStockPostingIndexManager? = null

/**
 * The global index for UniChatrooms
 */
@InternalAPI
@ExperimentalSerializationApi
var uniChatroomIndexManager: UniChatroomIndexManager? = null

/**
 * The global index for UniMessages
 */
@InternalAPI
@ExperimentalSerializationApi
var uniMessagesIndexManager: UniMessagesIndexManager? = null

/**
 * The global index for Snippets
 */
@InternalAPI
@ExperimentalSerializationApi
var snippetBaseIndexManager: SnippetBaseIndexManager? = null

/**
 * The global index for Knowledge
 */
@InternalAPI
@ExperimentalSerializationApi
var knowledgeIndexManager: KnowledgeIndexManager? = null

/**
 * The global index for Wisdom
 */
@InternalAPI
@ExperimentalSerializationApi
var wisdomIndexManager: WisdomIndexManager? = null

/**
 * The global index for Notification
 */
@InternalAPI
@ExperimentalSerializationApi
var notificationIndexManager: NotificationIndexManager? = null

/**
 * The global index for ProcessEvent
 */
@InternalAPI
@ExperimentalSerializationApi
var processIndexManager: ProcessIndexManager? = null

//*************************************************
//********************** TRACKER ******************
//*************************************************

@InternalAPI
@ExperimentalSerializationApi
var usageTracker: UsageTracker? = null

//*************************************************
//********************** MISCELLANEOUS ************
//*************************************************

//Instance title
var titleGlobal: String = "CWO ERP"

//Token for encryption/decryption
var tokenGlobal: String = ""

//Task
var taskJobGlobal: Job? = null

//File locations
val programPath: String = System.getProperty("user.dir")
var dataPath: String = ""
fun getModulePath(module: String) = Paths.get(dataPath, "data", module).toString()
fun getIniFile() = File(Paths.get(programPath, "cwo_erp.ini").toString())

//Search settings
var maxSearchResultsGlobal: Int = 2000

//Time
var differenceFromUTC: Int = 0

//Server/Client
@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@InternalAPI
@ExperimentalSerializationApi
lateinit var server: Server

var serverJobGlobal: Job? = null
var serverIPAddressGlobal: String = "?"

/**
 * Terminal supporting colors, animations etc.
 */
val terminal = Terminal(ansiLevel = AnsiLevel.TRUECOLOR)
