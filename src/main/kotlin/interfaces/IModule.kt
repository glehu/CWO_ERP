package interfaces

import api.logic.getCWOClient
import api.misc.json.EntryJson
import api.misc.json.EntryListJson
import db.CwODB
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
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
     * Used to save the entry to the database.
     */
    fun save(
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
                val index = indexManager.indexList[0]!!.indexMap[entry.uID]
                if (index != null) {
                    posDB = index.pos
                    byteSize = index.byteSize
                }
            }
            entry.initialize()
            val (posDBX, byteSizeX) = CwODB.saveEntry(
                entryBytes = encode(entry),
                uID = entry.uID,
                posDB = posDB,
                byteSize = byteSize,
                module = indexManager.module,
                raf = rafLocal
            )
            indexManager.indexEntry(entry, posDBX, byteSizeX, indexWriteToDisk, userName)
            if (raf == null) CwODB.closeRandomFileAccess(rafLocal)
            uID = entry.uID
            /**
             * Unlock the entry
             */
            getIndexManager()!!.setEntryLock(uID, false)
        } else {
            runBlocking {
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
        return decode(getBytes(uID))
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
        return decode(getBytes(uID))
    }

    /**
     * Used to retrieve a byte array of an entry from the database using the provided uID.
     * It is possible to retrieve an entry of another module if that module gets passed into the function.
     * @return the byte array of an entry with the provided unique identifier.
     */
    fun getBytes(uID: Int): ByteArray {
        var entryBytes: ByteArray = byteArrayOf()
        if (uID != -1) {
            if (!isClientGlobal) {
                val indexManager = getIndexManager()!!
                entryBytes = CwODB.getEntryFromUniqueID(
                    uID = uID,
                    module = indexManager.module,
                    index = indexManager.indexList[0]!!
                )
                setEntryLock(uID, true)
            } else {
                runBlocking {
                    launch {
                        entryBytes = getCWOClient().get("${getApiUrl()}entry/$uID?type=uid")
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

    fun getEntryBytesListJson(searchText: String, ixNr: Int): EntryListJson {
        val resultsListJson = EntryListJson(0, arrayListOf())
        var resultCounter = 0
        CwODB.getEntriesFromSearchString(
            searchText = searchText.uppercase(),
            ixNr = ixNr,
            exactSearch = false,
            indexManager = getIndexManager()!!
        ) { _, bytes ->
            resultCounter++
            resultsListJson.resultsList.add(bytes)
        }
        resultsListJson.resultsAmount = resultCounter
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

    fun log(logType: MXLog.LogType, text: String) {
        MXLog.log(
            module = module,
            type = logType,
            text = text,
            caller = moduleNameLong
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

    fun setEntryLock(uID: Int, locked: Boolean, userName: String = activeUser.username): Boolean {
        var success = false
        if (!isClientGlobal) {
            val indexManager = getIndexManager()!!
            val entryLocked = indexManager.getEntryLock(uID)
            if (locked) {
                if (!entryLocked) {
                    indexManager.getBaseIndex(uID).content = userName
                    success = true
                }
            } else {
                if (entryLocked) {
                    if (indexManager.getBaseIndex(uID).content == userName) {
                        indexManager.getBaseIndex(uID).content = "?"
                        success = true
                    }
                }
            }
        } else {
            runBlocking {
                launch {
                    success = getCWOClient().get("${getApiUrl()}setentrylock/$uID?type=$locked")
                }
            }
        }
        return success
    }
}