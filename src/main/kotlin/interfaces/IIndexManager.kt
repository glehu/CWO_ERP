package interfaces

import db.CwODB
import db.Index
import db.IndexContent
import io.ktor.util.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.MXLastChange
import modules.mx.activeUser
import modules.mx.getModulePath
import modules.mx.logic.MXLog
import modules.mx.logic.MXTimestamp.MXTimestamp.convUnixHexToUnixTimestamp
import modules.mx.logic.MXTimestamp.MXTimestamp.getLocalTimestamp
import modules.mx.logic.MXTimestamp.MXTimestamp.getUTCTimestamp
import modules.mx.logic.MXTimestamp.MXTimestamp.getUnixTimestampHex
import modules.mx.logic.indexFormat
import modules.mx.logic.roundTo
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.set
import kotlin.concurrent.thread

@InternalAPI
@ExperimentalSerializationApi
interface IIndexManager : IModule {
    val indexList: MutableMap<Int, Index>
    var lastUID: AtomicInteger
    var lastChangeDateHex: String
    var lastChangeDateUTC: String
    var lastChangeDateLocal: String
    var lastChangeUser: String
    var dbSizeKiByte: Double
    var ixSizeKiByte: Double

    /**
     * This function needs to be called in the init{} block of the index manager.
     */
    fun initialize(vararg ixNumbers: Int) {
        lastUID = updateLastUID()
        getLastChangeDates()
        addIndex(0)
        getIndicesFromArray(ixNumbers)
        getFileSizes()
    }

    fun getFileSizes() {
        dbSizeKiByte = (CwODB.getDatabaseFile(module).length()).toDouble() / 1024.0
        ixSizeKiByte = 0.0
        for (index in indexList.entries) {
            ixSizeKiByte += (getIndexFile(index.key).length()).toDouble() / 1024.0
        }
        dbSizeKiByte = dbSizeKiByte.roundTo(2)
        ixSizeKiByte = ixSizeKiByte.roundTo(2)
    }

    /**
     * @return a new unique identifier as an AtomicInteger.
     */
    fun getUID(): Int {
        val uID = lastUID.getAndIncrement()
        thread {
            setLastUniqueID(lastUID)
        }
        return uID
    }

    fun setLastChangeData(uID: Int, userName: String) {
        lastChangeDateHex = getUnixTimestampHex()
        val lastChange = MXLastChange(uID, lastChangeDateHex, userName)
        setLastChangeValues(lastChange)
        getLastChangeDates()
    }

    /**
     * Used to retrieve the last change dates from their unix hex values.
     */
    fun getLastChangeDates() {
        val lastChange = updateLastChangeData()
        lastChangeDateHex = lastChange.unixHex
        lastChangeUser = lastChange.user
        val unixLong = convUnixHexToUnixTimestamp(lastChangeDateHex)
        if (unixLong != 0L) {
            //UTC
            lastChangeDateUTC = getUTCTimestamp(unixLong)
            //Local
            lastChangeDateLocal = getLocalTimestamp(unixLong)
        } else {
            lastChangeDateHex = ""
            lastChangeDateUTC = ""
            lastChangeDateLocal = ""
        }
    }

    private fun updateLastUID() = getLastUniqueID()
    private fun updateLastChangeData() = getLastChange(module)

    /**
     * @return an ArrayList<String> of all available indices for searches.
     */
    fun getIndicesList(): ArrayList<String>

    /**
     * This function must call buildIndices, which will build all indices for an entry.
     */
    suspend fun indexEntry(
        entry: IEntry,
        posDB: Long,
        byteSize: Int,
        writeToDisk: Boolean,
        userName: String
    )

    /**
     * Used to build indices for an entry.
     */
    suspend fun buildIndices(
        uID: Int,
        posDB: Long,
        byteSize: Int,
        writeToDisk: Boolean,
        userName: String,
        vararg indices: Pair<Int, String>
    ) {
        buildDefaultIndex(uID, posDB, byteSize)
        for ((ixNr, ixContent) in indices) {
            if (ixContent != "?") {
                if (indexList[ixNr] == null) addIndex(ixNr)
                synchronized(this) {
                    indexList[ixNr]!!.indexMap[uID] = IndexContent(
                        content = indexFormat(ixContent).uppercase()
                    )
                }
            }
        }
        if (writeToDisk) {
            coroutineScope { launch { writeIndexData() } }
        }
        setLastChangeData(uID, userName)
    }

    /**
     * Builds the default index 0 (uID)
     */
    private fun buildDefaultIndex(uID: Int, posDB: Long, byteSize: Int) {
        if (indexList[0] == null) addIndex(0)
        indexList[0]!!.indexMap[uID] = IndexContent(pos = posDB, byteSize = byteSize)
    }

    /**
     * Used to add an index to the list of indices available.
     */
    private fun addIndex(ixNr: Int) {
        indexList[ixNr] = getIndex(ixNr)
    }

    /**
     * Writes the index values stored in the RAM into the database.
     */
    suspend fun writeIndexData() {
        synchronized(this) {
            for (index in indexList.entries) {
                getIndexFile(index.key).writeText(
                    Json.encodeToString(indexList[index.key])
                )
            }
            getFileSizes()
        }
    }

    fun getIndicesFromArray(ixNumbers: IntArray) {
        for (ixNr in ixNumbers) {
            addIndex(ixNr)
        }
    }

    /**
     * @return an instance of Index to be used in IndexManagers
     */
    fun getIndex(ixNr: Int): Index {
        log(MXLog.LogType.SYS, "Deserializing index $ixNr for $module...")
        checkIndexFile(module, ixNr)
        return Json.decodeFromString(getIndexFile(ixNr).readText())
    }

    private fun checkIndexFile(module: String, ixNr: Int): Boolean {
        var ok = false
        val nuPath = File(getModulePath(module))
        if (!nuPath.isDirectory) nuPath.mkdirs()
        val nuFile = getIndexFile(ixNr)
        if (!nuFile.isFile) {
            ok = false
            nuFile.createNewFile()
            //Now add an empty indexer to be used later
            getIndexFile(ixNr).writeText(Json.encodeToString(Index(module)))
            if (nuFile.isFile) ok = true
        }
        return ok
    }

    /**
     * Writes the last change date hex values to the disk
     */
    fun setLastChangeValues(lastChange: MXLastChange) {
        getLastChangeDateHexFile().writeText(Json.encodeToString(lastChange))
    }

    /**
     * @return the index file of a provided module
     */
    fun getIndexFile(ixNr: Int): File {
        return File("${getModulePath(module)}\\$module.ix$ixNr")
    }

    /**
     * @return the file storing the last change date hex value
     */
    private fun getLastChangeDateHexFile(): File {
        return File("${getModulePath(module)}\\lastchange.json")
    }

    /**
     * @return the last change data
     */
    fun getLastChange(module: String): MXLastChange {
        val lastChangeFile = getLastChangeDateHexFile()
        val lastChange: MXLastChange
        if (!lastChangeFile.isFile) {
            lastChangeFile.createNewFile()
            lastChange = MXLastChange(
                -1, getUnixTimestampHex(), activeUser.username
            )
            setLastChangeValues(lastChange)
        } else {
            lastChange = Json.decodeFromString(lastChangeFile.readText())
        }
        return lastChange
    }

    /**
     * Used to write the last unique identifier to the database.
     */
    fun setLastUniqueID(uniqueID: AtomicInteger) {
        getNuFile().writeText(uniqueID.toString())
    }

    /**
     * @return the last unique identifier as an AtomicInteger
     */
    fun getLastUniqueID(): AtomicInteger {
        checkNuFile()
        val nuFile = getNuFile()
        val lastUniqueIDNumber: AtomicInteger
        val lastUniqueIDString: String = nuFile.readText()
        lastUniqueIDNumber = if (lastUniqueIDString.isNotEmpty()) {
            AtomicInteger((lastUniqueIDString).toInt())
        } else AtomicInteger(0)
        return lastUniqueIDNumber
    }

    private fun getNuFile(): File {
        return File("${getModulePath(module)}\\$module.nu")
    }

    private fun checkNuFile(): Boolean {
        var ok = false
        val nuPath = File(getModulePath(module))
        if (!nuPath.isDirectory) nuPath.mkdirs()
        val nuFile = getNuFile()
        if (!nuFile.isFile) {
            ok = false
            nuFile.createNewFile()
            if (nuFile.isFile) ok = true
        }
        return ok
    }

    /**
     * @return the main index (ix0) for an entry's provided uID.
     */
    fun getBaseIndex(uID: Int): IndexContent {
        return indexList[0]!!.indexMap[uID]!!
    }

    fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean = false): String
}
