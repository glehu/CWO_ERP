package db

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.IIndexManager
import modules.IModule
import modules.mx.extractNumbers
import modules.mx.getModulePath
import modules.mx.indexFormat
import modules.mx.logic.MXLog
import tornadofx.Controller
import java.io.File
import java.io.RandomAccessFile

class CwODB : IModule, Controller()
{
    override fun moduleName() = "CwODB"

    //Settings
    private val finalMaxSearchResults = 2000

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
        //log("M1", ":> Indexing...")
        /* Since we are creating a new entry the entry's position in the database will be at the end
           To calculate "the end" we need the position and length of the last entry

           We will index the entry in both the sub indices (e.g. ix1 to index the entry name) and the modules.mx.main index
           The modules.mx.main index will only contain the most basic data uniqueID, posInDatabase and byteSize

           We will also store the latest added entry to determine the byteSize for the next added entry
           This will save a huge amount of time when adding new entries to an already big database
         */
        checkLastEntryFile(module)
        val lastEntryFile = getLastEntryFile(module)
        val indexText: String
        var posDBNew = 0L
        val previousByteSize: Long
        var indexError = false

        //If the byteSize of the new entry is greater than the old one we have to attach the new entry at the end
        if (!canOverride)
        {
            indexText = lastEntryFile.readText()
            if (indexText.isNotEmpty())
            {
                //First we separate the index' information [uniqueID;PosInDatabase;ByteSize] from the index' content {...}
                val indexInfoSeparator = "\\[[0-9]+;[0-9]+;[0-9]+]".toRegex()
                val indexInfo = indexInfoSeparator.find(indexText)
                if (indexInfo != null)
                {
                    //Now we get the previous PosInDatabase and ByteSize
                    val indexPosInDBSeparator = ";[0-9]+;".toRegex()
                    val indexPosInDB = indexPosInDBSeparator.find(indexInfo.value)
                    if (indexPosInDB != null)
                    {
                        val indexByteSizeSeparator = ";[0-9]+]".toRegex()
                        val indexByteSize = indexByteSizeSeparator.find(indexInfo.value)
                        if (indexByteSize != null)
                        {
                            previousByteSize = indexByteSize.value.extractNumbers().toLong()
                            posDBNew = indexPosInDB.value.extractNumbers().toLong()
                            //Now add the current entry's byteSize to the previous posInDatabase
                            posDBNew += previousByteSize
                        } else
                        {
                            MXLog.log(module, MXLog.LogType.ERROR, "Index error!", moduleName())
                            indexError = true
                        }
                    } else
                    {
                        MXLog.log(module, MXLog.LogType.ERROR, "Index error!", moduleName())
                        indexError = true
                    }
                } else
                {
                    MXLog.log(module, MXLog.LogType.ERROR, "Index error!", moduleName())
                    indexError = true
                }
            } else
            {
                posDBNew = 0L
            }
        } else
        {
            //Old entry can be overridden since the byteSize is less or equal to the old one
            posDBNew = posDB
        }

        if (!indexError)
        {
            //Save the serialized entry to the determined destination file
            writeDBEntry(entryBytes, posDBNew, raf)

            //If we saved a preexisting entry we have to delete the old entry entry
            //...if the new byteSize is greater than the old one
            if (posDB > -1L && !canOverride)
            {
                val emptyEntry = ByteArray(byteSize)
                val emptyRaf = openRandomFileAccess(module, "rw")
                writeDBEntry(emptyEntry, posDB, emptyRaf)
                closeRandomFileAccess(emptyRaf)
                MXLog.log(module, MXLog.LogType.INFO, "OLD_ENTRY_OVERRIDDEN", moduleName())
            }
            val indexContent = buildDBIndex(uID, posDBNew, byteSizeNew, false)
            getLastEntryFile(module).writeText(indexContent)
        } else
        {
            MXLog.log(module, MXLog.LogType.ERROR, "Serialization failed!", moduleName())
        }
        return Pair(posDBNew, byteSizeNew)
    }

    private fun buildDBIndex(uID: Int, posInDB: Long, byteSize: Int, extend: Boolean): String
    {
        var indexContent = "IX[$uID;$posInDB;$byteSize]"
        if (!extend)
        {
            indexContent += "\n"
        }
        return indexContent
    }

    @ExperimentalSerializationApi
    // Returns an array of all entries that fit the search criteria
    fun getEntriesFromSearchString(
        searchText: String, ixNr: Int, exactSearch: Boolean,
        module: String, maxSearchResults: Int = finalMaxSearchResults,
        indexManager: IIndexManager,
        updateProgress: (Int, ByteArray) -> Unit
    )
    {
        var counter = 0
        val searchString = indexFormat(searchText)
        var entryBytes: ByteArray

        if (getDatabaseFile(module).isFile)
        {
            val raf: RandomAccessFile = openRandomFileAccess(module, "r")
            //Determines the type of search that will be done depending on the search string
            val filteredMap: Map<Int, IndexContent> = if (searchString != "*" && searchString != "")
            {
                //Search text -> Search for specific entries
                indexManager.indexList[ixNr]!!.indexMap.filterValues { it.content.contains(searchString) }
            } else
            {
                //No search text -> Show all entries
                indexManager.indexList[ixNr]!!.indexMap
            }
            for ((key, indexContent) in filteredMap)
            {
                entryBytes = getDBEntry(indexContent.pos, indexContent.byteSize, raf)
                counter++
                //Callback
                updateProgress(key, entryBytes)
                if (maxSearchResults > -1 && counter >= maxSearchResults) break
            }
        }
    }

    fun openRandomFileAccess(module: String, mode: String) = RandomAccessFile(getDatabaseFile(module), mode)
    fun closeRandomFileAccess(randAccessFile: RandomAccessFile)
    {
        randAccessFile.close()
    }

    @ExperimentalSerializationApi
    fun getDBEntry(posInDatabase: Long, byteSize: Int, randAccessFile: RandomAccessFile): ByteArray
    {
        //We now read from the file
        val entry = ByteArray(byteSize)
        if (posInDatabase > 0)
        {
            randAccessFile.seek(posInDatabase)
        }
        randAccessFile.readFully(entry)
        return entry
    }

    @ExperimentalSerializationApi
    fun writeDBEntry(entry: ByteArray, posInDatabase: Long, raf: RandomAccessFile)
    {
        if (posInDatabase > 0)
        {
            raf.seek(posInDatabase)
        }
        raf.write(entry)
    }

    fun getUniqueID(module: String): Int
    {
        checkNuFile(module) //Check if <module>.nu exists; create if it doesn't
        var uniqueID = getLastUniqueID(module)
        uniqueID += 1
        setLastUniqueID(uniqueID, module)
        return uniqueID
    }

    private fun setLastUniqueID(uniqueID: Int, module: String)
    {
        val nuFile = getNuFile(module)
        val uniqueIDString = uniqueID.toString()
        nuFile.writeText(uniqueIDString)
    }

    fun getLastUniqueID(module: String): Int
    {
        val nuFile = getNuFile(module)
        val lastUniqueIDNumber: Int
        val lastUniqueIDString: String = nuFile.readText()
        lastUniqueIDNumber = if (lastUniqueIDString.isNotEmpty())
        {
            Integer.parseInt(lastUniqueIDString)
        } else
        {
            0
        }
        return lastUniqueIDNumber
    }

    private fun checkNuFile(module: String): Boolean
    {
        var ok = false
        val nuPath = File(getModulePath(module))
        if (!nuPath.isDirectory)
        {
            nuPath.mkdirs()
        }
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
        if (!nuPath.isDirectory)
        {
            nuPath.mkdirs()
        }
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
        if (!nuPath.isDirectory)
        {
            nuPath.mkdirs()
        }
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

    @ExperimentalSerializationApi
    fun getIndex(module: String, ixNr: Int): Index
    {
        MXLog.log(module, MXLog.LogType.INFO, "Deserializing index $ixNr for $module...", moduleName())
        checkIndexFile(module, ixNr)
        return Json.decodeFromString(getIndexFile(module, ixNr).readText())
    }

    fun getIndexFile(module: String, ixNr: Int) = File("${getModulePath(module)}\\$module.ix$ixNr")
    private fun getLastEntryFile(module: String) = File("${getModulePath(module)}\\lastentry.db")
    private fun getDatabaseFile(module: String) = File("${getModulePath(module)}\\$module.db")
    private fun getNuFile(module: String) = File("${getModulePath(module)}\\$module.nu")
}