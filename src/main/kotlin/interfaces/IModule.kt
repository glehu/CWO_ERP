package interfaces

import api.logic.getCWOClient
import api.misc.json.EntryBytesListJson
import api.misc.json.EntryJson
import api.misc.json.EntryListJson
import db.CwODB
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.Json
import modules.mx.activeUser
import modules.mx.isClientGlobal
import modules.mx.logic.MXLog
import modules.mx.protoBufGlobal
import java.io.RandomAccessFile

@ExperimentalSerializationApi
interface IModule {
    val moduleNameLong: String
    val module: String

    fun getIndexManager(): IIndexManager?

    fun getServerUrl(): String {
        return "http://${modules.mx.serverIPAddressGlobal}/"
    }

    fun getApiUrl(): String {
        return "${getServerUrl()}api/${module.lowercase()}/"
    }

    /**
     * Saves the entry to the database.
     * @return the uID of the entry.
     */
    suspend fun save(
        entry: IEntry,
        raf: RandomAccessFile? = null,
        indexWriteToDisk: Boolean = true,
        userName: String = activeUser.username
    ): Int {
        var uID = -1
        if (!isClientGlobal) {
            val indexManager = getIndexManager()!!
            val rafLocal = raf ?: CwODB.openRandomFileAccess(module, CwODB.CwODB.RafMode.READWRITE)
            var posDB: Long = -1L
            var byteSize: Int = -1
            if (entry.uID != -1) {
                val index = indexManager.getBaseIndex(entry.uID)
                posDB = index.pos
                byteSize = index.byteSize
            }
            entry.initialize()
            val posDBX: Long
            val byteSizeX: Int
            synchronized(this) {
                log(
                    logType = MXLog.LogType.SYS,
                    text = "SAVE START uID ${entry.uID}",
                    moduleAlt = indexManager.module
                )
                val (posDBXt, byteSizeXt) = CwODB.saveEntry(
                    entryBytes = encode(entry),
                    uID = entry.uID,
                    posDB = posDB,
                    byteSize = byteSize,
                    module = indexManager.module,
                    raf = rafLocal
                )
                posDBX = posDBXt
                byteSizeX = byteSizeXt
            }
            log(
                logType = MXLog.LogType.SYS,
                text = "SAVE END uID ${entry.uID}",
                moduleAlt = indexManager.module
            )
            indexManager.indexEntry(entry, posDBX, byteSizeX, indexWriteToDisk, userName)
            if (raf == null) CwODB.closeRandomFileAccess(rafLocal)
            uID = entry.uID
            /**
             * Unlock the entry
             */
            getIndexManager()!!.setEntryLock(uID, false)
        } else {
            coroutineScope {
                launch {
                    uID = getCWOClient().post("${getApiUrl()}saveentry") {
                        contentType(ContentType.Application.Json)
                        body = EntryJson(entry.uID, encode(entry))
                    }
                }
            }
        }
        return uID
    }

    /**
     * Used to retrieve an entry from the database using the provided uID.
     *
     * This function also locks the entry, making it uneditable for others. If this is not wanted,
     * use the load() function instead.
     * If an entry of another module needs to be retrieved, call this function with the controller of that module.
     * @return an entry with the provided unique identifier.
     */
    @ExperimentalSerializationApi
    fun get(uID: Int): IEntry {
        return decode(getBytes(uID, true))
    }

    /**
     * Used to retrieve an entry from the database using the provided uID.
     *
     * This function doesn't lock the entry, which means it cannot be saved.
     * If an entry of another module needs to be retrieved, call this function with the controller of that module.
     * @return an entry with the provided unique identifier.
     */
    @ExperimentalSerializationApi
    fun load(uID: Int): IEntry {
        return decode(getBytes(uID, false))
    }

    /**
     * Used to retrieve a byte array of an entry from the database using the provided uID.
     * It is possible to retrieve an entry of another module if that module gets passed into the function.
     * @return the byte array of an entry with the provided unique identifier.
     */
    fun getBytes(uID: Int, lock: Boolean = false, userName: String = activeUser.username): ByteArray {
        var entryBytes: ByteArray = byteArrayOf()
        if (uID != -1) {
            if (!isClientGlobal) {
                entryBytes = CwODB.getEntryFromUniqueID(
                    uID = uID,
                    indexManager = getIndexManager()!!
                )
                /**
                 * Lock the entry (if: GET)
                 */
                getIndexManager()!!.setEntryLock(uID, lock, userName)
            } else {
                runBlocking {
                    launch {
                        entryBytes = getCWOClient().get("${getApiUrl()}entry/$uID?type=uid&lock=$lock")
                    }
                }
            }
        }
        return entryBytes
    }

    /**
     * @return the encoded entry as a byte array.
     */
    fun encode(entry: IEntry): ByteArray {
        return protoBufGlobal.encodeToByteArray(entry)
    }

    /**
     * @return the decoded entry.
     */
    fun decode(entryBytes: ByteArray): IEntry {
        return protoBufGlobal.decodeFromByteArray(entryBytes)
    }

    fun getEntryBytesListJson(
        searchText: String,
        ixNr: Int,
        exactSearch: Boolean = false
    ): EntryBytesListJson {
        val resultsListJson = EntryBytesListJson(0, arrayListOf())
        var resultCounter = 0
        CwODB.getEntriesFromSearchString(
            searchText = searchText.uppercase(),
            ixNr = ixNr,
            exactSearch = exactSearch,
            indexManager = getIndexManager()!!
        ) { _, bytes ->
            resultCounter++
            resultsListJson.resultsList.add(bytes)
        }
        resultsListJson.total = resultCounter
        return resultsListJson
    }

    fun getEntryListJson(
        searchText: String,
        ixNr: Int,
        prettyPrint: Boolean = false,
        exactSearch: Boolean = false
    ): EntryListJson {
        val resultsListJson = EntryListJson(0, arrayListOf())
        var resultCounter = 0
        CwODB.getEntriesFromSearchString(
            searchText = searchText.uppercase(),
            ixNr = ixNr,
            exactSearch = exactSearch,
            indexManager = getIndexManager()!!
        ) { _, bytes ->
            resultCounter++
            resultsListJson.resultsList.add(
                getIndexManager()!!.encodeToJsonString(
                    entry = decode(bytes), prettyPrint = prettyPrint
                )
            )
        }
        resultsListJson.total = resultCounter
        return resultsListJson
    }

    /**
     * @return a list of indices the user can search in.
     */
    @ExperimentalSerializationApi
    fun getIndexUserSelection(): ArrayList<String> {
        lateinit var indexUserSelection: ArrayList<String>
        if (!isClientGlobal) {
            indexUserSelection = getIndexManager()!!.getIndicesList()
        } else {
            runBlocking {
                launch {
                    indexUserSelection = getCWOClient().get("${getApiUrl()}indexselection")
                }
            }
        }
        return indexUserSelection
    }

    /**
     * Displays text on the console and writes it to the module's log file.
     */
    fun log(logType: MXLog.LogType, text: String, apiEndpoint: String = "", moduleAlt: String? = null) {
        MXLog.log(
            module = moduleAlt ?: module,
            type = logType,
            text = text,
            caller = moduleNameLong,
            apiEndpoint = apiEndpoint
        )
    }

    fun getEntryLock(uID: Int, userName: String = activeUser.username): Boolean {
        var locked = false
        if (!isClientGlobal) {
            val content = getIndexManager()!!.getBaseIndex(uID).content
            locked = (content != "?" && content != userName)
        } else {
            runBlocking {
                launch {
                    locked = getCWOClient().get("${getApiUrl()}/getentrylock/$uID")
                }
            }
        }
        return locked
    }

    fun setEntryLock(uID: Int, doLock: Boolean, userName: String = activeUser.username): Boolean {
        var success = false
        if (!isClientGlobal) {
            val indexManager = getIndexManager()!!
            val entryLocked = indexManager.getEntryLock(uID, userName)
            if (doLock) {
                if (!entryLocked) {
                    indexManager.getBaseIndex(uID).content = userName
                    success = true
                }
            } else {
                /**
                 * If the entry is locked by the user that is trying to unlock -> unlock
                 */
                if (indexManager.getBaseIndex(uID).content == userName) {
                    indexManager.getBaseIndex(uID).content = "?"
                    success = true
                }
            }
        } else {
            runBlocking {
                launch {
                    success = getCWOClient().get("${getApiUrl()}setentrylock/$uID?type=$doLock")
                }
            }
        }
        return success
    }

    /**
     * Gets a json serializer with the option of pretty printing.
     * @return a json serializer.
     */
    fun json(prettyPrint: Boolean): Json {
        return Json { this.prettyPrint = prettyPrint }
    }
}
