package interfaces

import db.CwODB
import db.Index
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.MXLastChange
import modules.mx.MXUser
import modules.mx.logic.MXTimestamp.MXTimestamp.convUnixHexToUnixTimestamp
import modules.mx.logic.MXTimestamp.MXTimestamp.getLocalTimestamp
import modules.mx.logic.MXTimestamp.MXTimestamp.getUTCTimestamp
import modules.mx.logic.MXTimestamp.MXTimestamp.getUnixTimestampHex
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalSerializationApi
interface IIndexManager : IModule {
    val db: CwODB
    val indexList: Map<Int, Index>
    var lastUID: AtomicInteger
    var module: String
    var moduleDescription: String
    var lastChangeDateHex: String
    var lastChangeDateUTC: String
    var lastChangeDateLocal: String
    var lastChangeUser: String

    /**
     * @return a new unique identifier as an AtomicInteger.
     */
    fun getUID(): AtomicInteger {
        lastUID.getAndIncrement()
        db.setLastUniqueID(lastUID, module())
        return lastUID
    }

    /**
     * Deprecated
     */
    fun setLastChangeData(uID: Int, activeUser: MXUser) {
        lastChangeDateHex = getUnixTimestampHex()
        val lastChange = MXLastChange(uID, lastChangeDateHex, activeUser.username)
        db.setLastChangeValues(module, lastChange)
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

    /**
     * Used to format an input string to be used as an index value. Any non-alphanumerical character gets filtered out.
     */
    fun indexFormat(text: String): String {
        val songNameArray = text.uppercase(Locale.getDefault()).toCharArray()
        var formatted = ""
        for (i in songNameArray.indices) {
            //Only alphanumerical characters (letters and numbers)
            val regex = "^[A-Z]?[0-9]?$".toRegex()
            if (regex.matches(songNameArray[i].toString())) {
                formatted += songNameArray[i]
            }
        }
        return formatted
    }

    fun updateLastUID() = db.getLastUniqueID(module())
    fun updateLastChangeData() = db.getLastChange(module())

    /**
     * @return an ArrayList<String> of all available indices for searches.
     */
    fun getIndexUserSelection(): ArrayList<String>

    /**
     * Used to generate all indices for an entry.
     */
    fun indexEntry(entry: Any, posDB: Long, byteSize: Int, writeToDisk: Boolean = true)
    fun buildIndex0(entry: Any, posDB: Long, byteSize: Int)

    /**
     * Writes the index values stored in the RAM into the database.
     */
    suspend fun writeIndexData()
}