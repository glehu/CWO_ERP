package db

import interfaces.IIndexManager
import interfaces.IModule
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.MXLastChange
import modules.mx.activeUser
import modules.mx.getModulePath
import modules.mx.logic.MXLog
import modules.mx.logic.MXTimestamp.MXTimestamp.getUnixTimestampHex
import modules.mx.maxSearchResultsGlobal
import tornadofx.Controller
import java.io.File
import java.io.RandomAccessFile

class CwODB : IModule, Controller()
{
    override fun moduleNameLong() = "CwODB"
    override fun module() = "DB"

    @ExperimentalSerializationApi
    fun saveEntry(
        entryBytes: ByteArray, uID: Int, posDB: Long, byteSize: Int,
        module: String, raf: RandomAccessFile
    ): Pair<Long, Int>
    {
        //Save the new byteSize to determine if the old entry can be overridden with the new entry (<= size)
        //instead of adding the new entry at the end of the database (> size)
        val byteSizeNew = entryBytes.size
        var canOverride = false
        if (byteSizeNew <= byteSize)
        {
            canOverride = true
        }
        //Loading the database file for the entry to be saved in
        /* Since we are creating a new entry the entry's position in the database will be at the end
           To calculate "the end" we need the position and length of the last entry

           We will index the entry in both the sub indices (e.g. ix1 to index the entry name) and the main index
           The main index will only contain the most basic data uniqueID, posInDatabase and byteSize

           We will also store the latest added entry to determine the byteSize for the next added entry
           This will save a huge amount of time when adding new entries to an already big database
         */
        checkLastEntryFile(module)
        val lastEntryFile = getLastEntryFile(module)
        val indexText: String
        var posDBNew: Long
        val previousByteSize: Long
        val indexError = false

        //If the byteSize of the new entry is greater than the old one we have to attach the new entry at the end
        if (!canOverride)
        {
            indexText = lastEntryFile.readText()
            if (indexText.isNotEmpty())
            {
                val lastEntryIndex = Json.decodeFromString<IndexContent>(indexText)
                //Now we get the previous PosInDatabase and ByteSize
                val indexPosInDB = lastEntryIndex.pos
                val indexByteSize = lastEntryIndex.byteSize
                previousByteSize = indexByteSize.toLong()
                posDBNew = indexPosInDB
                //Now add the current entry's byteSize to the previous posInDatabase
                posDBNew += previousByteSize
            } else posDBNew = 0L
        } else
        {
            //Old entry can be overridden since the byteSize is less or equal to the old one
            posDBNew = posDB
        }

        if (!indexError)
        {
            //Save the serialized entry to the determined destination file
            writeDBEntry(entryBytes, posDBNew, raf)

            //If we saved a preexisting entry we have to delete the old entry
            //...if the new byteSize is greater than the old one
            if (posDB > -1L && !canOverride)
            {
                val emptyEntry = ByteArray(byteSize)
                val emptyRaf = openRandomFileAccess(module, RafMode.READWRITE)
                writeDBEntry(emptyEntry, posDB, emptyRaf)
                closeRandomFileAccess(emptyRaf)
            }
            if (!canOverride)
            {
                getLastEntryFile(module).writeText(
                    Json.encodeToString(IndexContent(uID, "", posDBNew, byteSizeNew))
                )
            }
        } else MXLog.log(module, MXLog.LogType.ERROR, "Serialization failed!", moduleNameLong())
        return Pair(posDBNew, byteSizeNew)
    }

    @ExperimentalSerializationApi
    // Returns an array of all entries that fit the search criteria
    fun getEntriesFromSearchString(
        searchText: String, ixNr: Int, exactSearch: Boolean,
        module: String, maxSearchResults: Int = maxSearchResultsGlobal,
        indexManager: IIndexManager,
        updateProgress: (Int, ByteArray) -> Unit
    )
    {
        var counter = 0
        var entryBytes: ByteArray

        if (getDatabaseFile(module).isFile)
        {
            val raf: RandomAccessFile = openRandomFileAccess(module, RafMode.READ)
            //Determines the type of search that will be done depending on the search string
            val filteredMap: Map<Int, IndexContent> = if (!isGetAll(searchText))
            {
                //Search text -> Search for specific entries
                if (exactSearch)
                {
                    //Literal search
                    indexManager.indexList[ixNr]!!.indexMap.filterValues {
                        it.content.contains(searchText)
                    }
                } else
                {
                    //Regex search
                    indexManager.indexList[ixNr]!!.indexMap.filterValues {
                        it.content.contains(searchText.toRegex())
                    }
                }
            } else
            {
                //No search text -> Show all entries
                indexManager.indexList[ixNr]!!.indexMap
            }
            for ((key, indexContent) in filteredMap)
            {
                entryBytes = readDBEntry(indexContent.pos, indexContent.byteSize, raf)
                counter++
                //Callback
                updateProgress(key, entryBytes)
                if (maxSearchResults > -1 && counter >= maxSearchResults) break
            }
        }
    }

    private fun isGetAll(searchText: String): Boolean
    {
        var getAll = false
        when (searchText)
        {
            "" -> getAll = true
            "*" -> getAll = true
        }
        return getAll
    }

    @ExperimentalSerializationApi
    fun getEntryFromUniqueID(uID: Int, module: String, index: Index): ByteArray
    {
        lateinit var entryBytes: ByteArray
        if (getDatabaseFile(module).isFile)
        {
            val raf: RandomAccessFile = openRandomFileAccess(module, RafMode.READ)
            val indexContent = index.indexMap[uID]
            entryBytes = readDBEntry(indexContent!!.pos, indexContent.byteSize, raf)
        }
        return entryBytes
    }

    enum class RafMode
    {
        READ, WRITE, READWRITE
    }

    fun openRandomFileAccess(module: String, mode: RafMode): RandomAccessFile
    {
        val rafMode: String = when(mode)
        {
            RafMode.READ -> "r"
            RafMode.WRITE -> "w"
            RafMode.READWRITE -> "rw"
        }
        return RandomAccessFile(getDatabaseFile(module), rafMode)
    }
    fun closeRandomFileAccess(randAccessFile: RandomAccessFile) = randAccessFile.close()

    @ExperimentalSerializationApi
    fun readDBEntry(posInDatabase: Long, byteSize: Int, randAccessFile: RandomAccessFile): ByteArray
    {
        //We now read from the file
        val entry = ByteArray(byteSize)
        if (posInDatabase > 0) randAccessFile.seek(posInDatabase)
        randAccessFile.readFully(entry)
        return entry
    }

    @ExperimentalSerializationApi
    fun writeDBEntry(entry: ByteArray, posInDatabase: Long, raf: RandomAccessFile)
    {
        if (posInDatabase > 0) raf.seek(posInDatabase)
        raf.write(entry)
    }

    fun setLastUniqueID(uniqueID: Int, module: String)
    {
        val nuFile = getNuFile(module)
        val uniqueIDString = uniqueID.toString()
        nuFile.writeText(uniqueIDString)
    }

    fun getLastUniqueID(module: String): Int
    {
        checkNuFile(module)
        val nuFile = getNuFile(module)
        val lastUniqueIDNumber: Int
        val lastUniqueIDString: String = nuFile.readText()
        lastUniqueIDNumber = if (lastUniqueIDString.isNotEmpty())
        {
            Integer.parseInt(lastUniqueIDString)
        } else 0
        return lastUniqueIDNumber
    }

    private fun checkNuFile(module: String): Boolean
    {
        var ok = false
        val nuPath = File(getModulePath(module))
        if (!nuPath.isDirectory) nuPath.mkdirs()
        val nuFile = getNuFile(module)
        if (!nuFile.isFile)
        {
            ok = false
            nuFile.createNewFile()
            if (nuFile.isFile) ok = true
        }
        return ok
    }

    private fun checkLastEntryFile(module: String): Boolean
    {
        var ok = false
        val nuPath = File(getModulePath(module))
        if (!nuPath.isDirectory) nuPath.mkdirs()
        val nuFile = getLastEntryFile(module)
        if (!nuFile.isFile)
        {
            ok = false
            nuFile.createNewFile()
            if (nuFile.isFile) ok = true
        }
        return ok
    }

    @ExperimentalSerializationApi
    fun checkIndexFile(module: String, ixNr: Int): Boolean
    {
        var ok = false
        val nuPath = File(getModulePath(module))
        if (!nuPath.isDirectory) nuPath.mkdirs()
        val nuFile = getIndexFile(module, ixNr)
        if (!nuFile.isFile)
        {
            ok = false
            nuFile.createNewFile()
            //Now add an empty indexer to be used later
            getIndexFile(module, ixNr).writeText(Json.encodeToString(Index(module)))
            if (nuFile.isFile) ok = true
        }
        return ok
    }

    fun resetModuleDatabase(module: String)
    {
        if (File(getModulePath(module)).isDirectory)
        {
            File("${getModulePath(module)}\\$module.db").delete()
            File("${getModulePath(module)}\\$module.nu").delete()
            for (i in 0..99) File("${getModulePath(module)}\\$module.ix$i").delete()
            MXLog.log(module, MXLog.LogType.INFO, "Reset database for $module successful", moduleNameLong())
        }
    }

    @ExperimentalSerializationApi
    fun getIndex(module: String, ixNr: Int): Index
    {
        MXLog.log(module, MXLog.LogType.INFO, "Deserializing index $ixNr for $module...", moduleNameLong())
        checkIndexFile(module, ixNr)
        return Json.decodeFromString(getIndexFile(module, ixNr).readText())
    }

    fun getLastChange(module: String): MXLastChange
    {
        val lastChangeFile = getLastChangeDateHexFile(module)
        val lastChange: MXLastChange
        if (!lastChangeFile.isFile)
        {
            lastChangeFile.createNewFile()
            lastChange = MXLastChange(
                -1, getUnixTimestampHex(), activeUser.username
            )
            setLastChangeValues(module, lastChange)
        } else
        {
            lastChange = Json.decodeFromString(lastChangeFile.readText())
        }
        return lastChange
    }

    fun setLastChangeValues(module: String, lastChange: MXLastChange)
    {
        getLastChangeDateHexFile(module).writeText(Json.encodeToString(lastChange))
    }

    fun getIndexFile(module: String, ixNr: Int) = File("${getModulePath(module)}\\$module.ix$ixNr")
    private fun getLastEntryFile(module: String) = File("${getModulePath(module)}\\lastentry.db")
    private fun getDatabaseFile(module: String) = File("${getModulePath(module)}\\$module.db")
    private fun getNuFile(module: String) = File("${getModulePath(module)}\\$module.nu")
    private fun getLastChangeDateHexFile(module: String) = File("${getModulePath(module)}\\lastchange.json")
}