package interfaces

import db.Index
import db.IndexContent
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalSerializationApi
interface IIndexManager : IModule {
    val indexList: MutableMap<Int, Index>
    var lastUID: AtomicInteger
    var lastChangeDateHex: String
    var lastChangeDateUTC: String
    var lastChangeDateLocal: String
    var lastChangeUser: String

    /**
     * This function needs to be called in the init{} block of the index manager.
     */
    fun initialize() {
        lastUID = updateLastUID()
        getLastChangeDates()
    }

    /**
     * @return a new unique identifier as an AtomicInteger.
     */
    fun getUID(): AtomicInteger {
        lastUID.getAndIncrement()
        setLastUniqueID(lastUID)
        return lastUID
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
    fun indexEntry(
        entry: IEntry,
        posDB: Long,
        byteSize: Int,
        writeToDisk: Boolean,
        userName: String
    )

    /**
     * Used to build indices for an entry.
     */
    fun buildIndices(
        uID: Int,
        posDB: Long,
        byteSize: Int,
        writeToDisk: Boolean,
        userName: String,
        vararg indices: Pair<Int, String>
    ) {
        for ((ixNr, ixContent) in indices) {
            if (indexList[ixNr] == null) addIndex(ixNr)
            indexList[ixNr]!!.indexMap[uID] = IndexContent(
                uID,
                indexFormat(ixContent).uppercase(),
                posDB,
                byteSize
            )
        }
        if (writeToDisk) runBlocking {
            launch { writeIndexData() }
        }
        setLastChangeData(uID, userName)
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
        for (index in indexList.entries) {
            getIndexFile(index.key).writeText(
                Json.encodeToString(indexList[index.key])
            )
        }
    }

    /**
     * @return an instance of Index to be used in IndexManagers
     */
    fun getIndex(ixNr: Int): Index {
        log(MXLog.LogType.INFO, "Deserializing index $ixNr for $module...")
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
        val nuFile = getNuFile()
        val uniqueIDString = uniqueID.toString()
        nuFile.writeText(uniqueIDString)
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
}