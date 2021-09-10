package interfaces

import db.CwODB
import db.Index
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
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalSerializationApi
interface IIndexManager : IModule {
    val indexList: Map<Int, Index>
    var lastUID: AtomicInteger
    var lastChangeDateHex: String
    var lastChangeDateUTC: String
    var lastChangeDateLocal: String
    var lastChangeUser: String

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

    fun updateLastUID() = getLastUniqueID()
    fun updateLastChangeData() = getLastChange(module)

    /**
     * @return an ArrayList<String> of all available indices for searches.
     */
    fun getIndexUserSelection(): ArrayList<String>

    /**
     * Used to generate all indices for an entry.
     */
    fun indexEntry(
        entry: Any,
        posDB: Long,
        byteSize: Int,
        writeToDisk: Boolean = true,
        userName: String
    )

    fun buildIndex0(entry: Any, posDB: Long, byteSize: Int)

    /**
     * Writes the index values stored in the RAM into the database.
     */
    suspend fun writeIndexData()

    /**
     * @return an instance of Index to be used in IndexManagers
     */
    fun getIndex(ixNr: Int): Index {
        MXLog.log(module, MXLog.LogType.INFO, "Deserializing index $ixNr for $module...", CwODB.moduleNameLong)
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