package db

import interfaces.IIndexManager
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.getModulePath
import modules.mx.maxSearchResultsGlobal
import tornadofx.Controller
import java.io.File
import java.io.RandomAccessFile

@ExperimentalSerializationApi
class CwODB {
    companion object CwODB : Controller() {
        /**
         * Used to save the ByteArray of an entry (achieved by serialization) and store it in the database.
         * @return the position in the database and the byte size of the stored entry
         */
        fun saveEntry(
            entryBytes: ByteArray,
            uID: Int,
            posDB: Long,
            byteSize: Int,
            module: String,
            raf: RandomAccessFile
        ): Pair<Long, Int> {
            val byteSizeNew = entryBytes.size
            var canOverride = false
            if (byteSizeNew <= byteSize) canOverride = true
            checkLastEntryFile(module)
            val lastEntryFile = getLastEntryFile(module)
            val indexText: String
            var posDBNew: Long
            val previousByteSize: Long
            val indexError = false
            //If the byteSize of the new entry is greater than the old one we have to attach the new entry at the end
            if (!canOverride) {
                indexText = lastEntryFile.readText()
                if (indexText.isNotEmpty()) {
                    val lastEntryIndex = Json.decodeFromString<IndexContent>(indexText)
                    //Now we get the previous PosInDatabase and ByteSize
                    val indexPosInDB = lastEntryIndex.pos
                    val indexByteSize = lastEntryIndex.byteSize
                    previousByteSize = indexByteSize.toLong()
                    posDBNew = indexPosInDB
                    //Now add the current entry's byteSize to the previous posInDatabase
                    posDBNew += previousByteSize
                } else posDBNew = 0L
            } else {
                //Old entry can be overridden since the byteSize is less or equal to the old one
                posDBNew = posDB
            }
            if (!indexError) {
                //Save the serialized entry to the determined destination file
                writeDBEntry(entryBytes, posDBNew, raf)

                //If we saved a preexisting entry we have to delete the old entry
                //...if the new byteSize is greater than the old one
                if (posDB > -1L && !canOverride) {
                    val emptyEntry = ByteArray(byteSize)
                    val emptyRaf = openRandomFileAccess(module, RafMode.READWRITE)
                    writeDBEntry(emptyEntry, posDB, emptyRaf)
                    closeRandomFileAccess(emptyRaf)
                }
                if (!canOverride) {
                    getLastEntryFile(module).writeText(
                        Json.encodeToString(IndexContent(uID, "", posDBNew, byteSizeNew))
                    )
                }
            }
            return Pair(posDBNew, byteSizeNew)
        }

        /**
         * Used to search for entries in the database with the provided search string and the index number.
         * @return ByteArray of matched entry as callback for all matched entries.
         */
        fun getEntriesFromSearchString(
            searchText: String,
            ixNr: Int,
            exactSearch: Boolean,
            maxSearchResults: Int = maxSearchResultsGlobal,
            indexManager: IIndexManager,
            module: String = indexManager.module,
            updateProgress: (Int, ByteArray) -> Unit
        ) {
            var counter = 0
            var entryBytes: ByteArray
            if (getDatabaseFile(module).isFile) {
                val raf: RandomAccessFile = openRandomFileAccess(module, RafMode.READ)
                val filteredMap: Map<Int, IndexContent> = if (!isGetAll(searchText)) {
                    if (exactSearch) {
                        /**
                         * Searches in the provided index
                         */
                        indexManager.indexList[ixNr]!!.indexMap.filterValues {
                            it.content.contains(searchText)
                        }
                    } else {
                        /**
                         * Searches in all available indices
                         */
                        returnFromAllIndices(indexManager, searchText)
                    }
                } else {
                    //No search text -> Show all entries
                    indexManager.indexList[0]!!.indexMap
                }
                for (uID in filteredMap.keys) {
                    val baseIndex = indexManager.getBaseIndex(uID)
                    entryBytes = readDBEntry(baseIndex.pos, baseIndex.byteSize, raf)
                    counter++
                    //Callback
                    updateProgress(uID, entryBytes)
                    if (maxSearchResults > -1 && counter >= maxSearchResults) break
                }
            }
        }

        private fun returnFromAllIndices(indexManager: IIndexManager, searchText: String): Map<Int, IndexContent> {
            val results = mutableMapOf<Int, IndexContent>()
            for (ixNr in 1 until indexManager.indexList.size)
                results.putAll(indexManager.indexList[ixNr]!!.indexMap.filterValues {
                    it.content.contains(searchText.toRegex())
                })
            return results.toSortedMap(compareBy<Int> { it })
        }

        private fun isGetAll(searchText: String): Boolean {
            var getAll = false
            when (searchText) {
                "" -> getAll = true
                "*" -> getAll = true
            }
            return getAll
        }

        /**
         * Used to retrieve a single entry with the provided unique identifier.
         * @return the ByteArray of the entry
         */
        fun getEntryFromUniqueID(uID: Int, module: String, index: Index): ByteArray {
            lateinit var entryBytes: ByteArray
            if (getDatabaseFile(module).isFile) {
                val raf: RandomAccessFile = openRandomFileAccess(module, RafMode.READ)
                val indexContent = index.indexMap[uID]
                entryBytes = readDBEntry(indexContent!!.pos, indexContent.byteSize, raf)
                closeRandomFileAccess(raf)
            }
            return entryBytes
        }

        enum class RafMode {
            READ, WRITE, READWRITE
        }

        /**
         * Used to open a new RandomAccessFile instance, e.g. to initiate the process of saving a new entry.
         * The usage has to be declared by providing one of the RafModes (READ, WRITE or READWRITE)
         * @return a RandomAccessFile instance
         */
        fun openRandomFileAccess(module: String, mode: RafMode): RandomAccessFile {
            val rafMode: String = when (mode) {
                RafMode.READ -> "r"
                RafMode.WRITE -> "w"
                RafMode.READWRITE -> "rw"
            }
            return RandomAccessFile(getDatabaseFile(module), rafMode)
        }

        /**
         * Used to close a previously created instance of a RandomAccessFile.
         */
        fun closeRandomFileAccess(randAccessFile: RandomAccessFile) {
            randAccessFile.close()
        }

        /**
         * Reads the serialized entry bytes directly from the disk.
         * @return ByteArray of Google ProtoBuf serialized entry.
         */
        private fun readDBEntry(posInDatabase: Long, byteSize: Int, randAccessFile: RandomAccessFile): ByteArray {
            val entry = ByteArray(byteSize)
            if (posInDatabase > 0) randAccessFile.seek(posInDatabase)
            randAccessFile.readFully(entry)
            return entry
        }

        /**
         * Writes the serialized entry bytes directly to the disk.
         */
        private fun writeDBEntry(entry: ByteArray, posInDatabase: Long, raf: RandomAccessFile) {
            if (posInDatabase > 0) raf.seek(posInDatabase)
            raf.write(entry)
        }

        /**
         * Used to reset a module's database including index files. Logfiles remain untouched by this operation.
         */
        fun resetModuleDatabase(module: String) {
            val modulePath = getModulePath(module)
            if (File(modulePath).isDirectory) {
                File("$modulePath\\$module.db").delete()
                File("$modulePath\\$module.nu").delete()
                for (i in 0..99) File("$modulePath\\$module.ix$i").delete()
            }
        }

        private fun checkLastEntryFile(module: String): Boolean {
            var ok = false
            val nuPath = File(getModulePath(module))
            if (!nuPath.isDirectory) nuPath.mkdirs()
            val nuFile = getLastEntryFile(module)
            if (!nuFile.isFile) {
                ok = false
                nuFile.createNewFile()
                if (nuFile.isFile) ok = true
            }
            return ok
        }

        private fun getLastEntryFile(module: String): File {
            return File("${getModulePath(module)}\\lastentry.db")
        }

        /**
         * Retrieves a specified module's database file.
         * @return the database file.
         */
        fun getDatabaseFile(module: String): File {
            return File("${getModulePath(module)}\\$module.db")
        }
    }
}