package modules

import db.CwODB
import db.Index
import modules.mx.MXLastChange
import modules.mx.MXUser
import modules.mx.differenceFromUTC
import java.util.*
import kotlin.collections.ArrayList

interface IIndexManager : IModule
{
    val db: CwODB
    val indexList: Map<Int, Index>
    var lastUID: Int
    var module: String
    var moduleDescription: String
    var lastChangeDateHex: String
    var lastChangeDateUTC: String
    var lastChangeDateLocal: String
    var lastChangeUser: String

    fun getUID(): Int
    {
        lastUID++
        db.setLastUniqueID(lastUID, module())
        return lastUID
    }

    fun setLastChangeData(uID: Int, activeUser: MXUser)
    {
        lastChangeDateHex = (System.currentTimeMillis() / 1000).toString(16)
        val lastChange = MXLastChange(uID, lastChangeDateHex, activeUser.username)
        db.setLastChangeValues(module, lastChange)
        getLastChangeDates()
    }

    fun getLastChangeDates()
    {
        val lastChange = updateLastChangeData()
        lastChangeDateHex = lastChange.unixHex
        lastChangeUser = lastChange.user
        val unixLong = java.lang.Long.parseLong(lastChangeDateHex, 16)
        if (unixLong != 0L)
        {
            //UTC
            lastChangeDateUTC = java.time.format.DateTimeFormatter.ISO_INSTANT
                .format(java.time.Instant.ofEpochSecond(unixLong))
            //Local
            lastChangeDateLocal = java.time.format.DateTimeFormatter.ISO_INSTANT
                .format(java.time.Instant.ofEpochSecond(unixLong + (differenceFromUTC * 3600)))
        } else
        {
            lastChangeDateHex = ""
            lastChangeDateUTC = ""
            lastChangeDateLocal = ""
        }
    }

    fun indexFormat(text: String): String
    {
        val songNameArray = text.uppercase(Locale.getDefault()).toCharArray()
        var formatted = ""
        for (i in songNameArray.indices)
        {
            //Only alphanumerical characters (letters and numbers)
            val regex = "^[A-Z]?[0-9]?$".toRegex()
            if (regex.matches(songNameArray[i].toString()))
            {
                formatted += songNameArray[i]
            }
        }
        return formatted
    }

    fun updateLastUID() = db.getLastUniqueID(module())
    fun updateLastChangeData() = db.getLastChange(module())
    fun getIndexUserSelection(): ArrayList<String>
    fun indexEntry(entry: Any, posDB: Long, byteSize: Int, writeToDisk: Boolean = true)
    fun buildIndex0(entry: Any, posDB: Long, byteSize: Int)
    suspend fun writeIndexData()
}