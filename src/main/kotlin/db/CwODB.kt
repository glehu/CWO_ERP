package db

import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.getModulePath
import modules.mx.maxSearchResultsGlobal
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Paths

@ExperimentalSerializationApi
class CwODB {
  @InternalAPI
  companion object CwODB {
    /**
     * Used to save the ByteArray of an entry (achieved by serialization) and store it in the database.
     * @return the position in the database and byte size of the stored entry.
     */
    fun saveEntry(
      entryBytes: ByteArray,
      posDB: Long,
      byteSize: Int,
      module: String,
      raf: RandomAccessFile
    ): Pair<Long, Int> {
      val byteSizeNew = entryBytes.size
      var canOverride = false
      if (byteSizeNew <= byteSize) canOverride = true
      var posDBNew: Long
      val indexError = false
      synchronized(this) {
        //If the byteSize of the new entry is greater than the old one we have to attach the new entry at the end
        posDBNew = if (!canOverride) {
          getDatabaseFile(module).length()
        } else {
          //Old entry can be overridden since the byteSize is less or equal to the old one
          posDB
        }
        if (!indexError) {
          //Save the serialized entry to the determined destination file
          writeDBEntry(entryBytes, posDBNew, raf)
          /*
           If we saved a preexisting entry we have to delete the old entry
           ...if the new byteSize is greater than the old one
           */
          if (posDB > -1L && !canOverride) {
            val emptyEntry = ByteArray(byteSize)
            val emptyRaf = openRandomFileAccess(module, RafMode.READWRITE)
            writeDBEntry(emptyEntry, posDB, emptyRaf)
            closeRandomFileAccess(emptyRaf)
          }
        }
      }
      return Pair(posDBNew, byteSizeNew)
    }

    /**
     * Used to search for entries in the database with the provided search string and the index number.
     * @return the ByteArray of each matched entry as a callback for all matched entries.
     */
    fun getEntriesFromSearchString(
      searchText: String,
      ixNr: Int,
      exactSearch: Boolean,
      maxSearchResults: Int = maxSearchResultsGlobal,
      indexManager: IIndexManager,
      module: String = indexManager.module,
      numberComparison: Boolean = false,
      updateProgress: (Int, ByteArray) -> Unit
    ) {
      var counter = 0
      var entryBytes: ByteArray
      if (getDatabaseFile(module).isFile) {
        val raf: RandomAccessFile = openRandomFileAccess(module, RafMode.READ)
        val filteredMap: Map<Int, IndexContent>
        if (!isGetAll(searchText)) {
          if (!exactSearch) {
            //Searches in all available indices
            returnFromAllIndices(indexManager, searchText) { indexResult ->
              for (uID in indexResult.keys) {
                val baseIndex = indexManager.getBaseIndex(uID)
                entryBytes = readDBEntry(baseIndex.pos, baseIndex.byteSize, raf)
                if (entryBytes.isEmpty()) continue
                counter++
                //Callback
                updateProgress(uID, entryBytes)
                if (maxSearchResults > -1 && counter >= maxSearchResults) break
              }
            }
            return
          } else {
            //Searches in the provided index
            filteredMap = if (!numberComparison) {
              indexManager.indexList[ixNr]!!.indexMap.filterValues {
                it.content.contains(searchText.toRegex())
              }
            } else {
              indexManager.indexList[ixNr]!!.indexMap.filterValues {
                it.content.toDouble() >= searchText.toDouble()
              }
            }
          }
        } else {
          //No search text -> Show all entries
          filteredMap = indexManager.indexList[0]!!.indexMap
        }
        for (uID in filteredMap.keys) {
          val baseIndex = indexManager.getBaseIndex(uID)
          entryBytes = readDBEntry(baseIndex.pos, baseIndex.byteSize, raf)
          if (entryBytes.isEmpty()) continue
          counter++
          //Callback
          updateProgress(uID, entryBytes)
          if (maxSearchResults > -1 && counter >= maxSearchResults) break
        }
      }
    }

    /**
     * @return the index results for a search text from all available indices.
     */
    private fun returnFromAllIndices(
      indexManager: IIndexManager,
      searchText: String,
      updateProgress: (Map<Int, IndexContent>) -> Unit
    ) {
      runBlocking {
        searchInAllIndices(indexManager, searchText).collect { indexResult ->
          updateProgress(indexResult)
        }
      }
    }

    /**
     * Used to search in all indices by starting an index search flow for each index manager.
     * @return the flow of an index manager's index search.
     */
    private fun searchInAllIndices(indexManager: IIndexManager, searchText: String): Flow<Map<Int, IndexContent>> =
      flow {
        for (ixNr in 1 until indexManager.indexList.size) {
          val results = indexManager.indexList[ixNr]!!.indexMap.filterValues {
            it.content.contains(searchText.toRegex())
          }
          emit(results)
        }
      }

    /**
     * Used to determine if a text search needs to be done or if all entries can be retrieved.
     * @return true if all entries will be retrieved and false if there will be a text search.
     */
    private fun isGetAll(searchText: String): Boolean {
      return searchText == "*"
    }

    /**
     * Used to retrieve a single entry with the provided unique identifier.
     * @return the ByteArray of the entry
     */
    fun getEntryByteArrayFromUID(uID: Int, indexManager: IIndexManager): ByteArray {
      lateinit var entryBytes: ByteArray
      if (getDatabaseFile(indexManager.module).isFile) {
        val raf: RandomAccessFile = openRandomFileAccess(indexManager.module, RafMode.READ)
        val indexContent = indexManager.getBaseIndex(uID)
        entryBytes = readDBEntry(indexContent.pos, indexContent.byteSize, raf)
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
      var entry = ByteArray(byteSize)
      try {
        if (posInDatabase > 0) randAccessFile.seek(posInDatabase)
        randAccessFile.readFully(entry)
      } catch (e: java.io.EOFException) {
        println(e.message)
        entry = ByteArray(0)
      } catch (e: java.io.IOException) {
        println(e.message)
        entry = ByteArray(0)
      }
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
     * Used to reset a module's database including index files.
     *
     * Logfiles remain untouched by this operation.
     * @return true, if the operation was successful.
     */
    fun resetModuleDatabase(module: String): Boolean {
      val modulePath = getModulePath(module)
      return if (File(modulePath).isDirectory) {
        File(Paths.get(modulePath, "$module.db").toString()).delete()
        File(Paths.get(modulePath, "$module.nu").toString()).delete()
        File(Paths.get(modulePath, "lastentry.db").toString()).delete()
        for (i in 0..99) {
          val file = File(Paths.get(modulePath, "$module.ix$i").toString())
          if (file.exists()) file.delete()
        }
        true
      } else false
    }

    /**
     * Retrieves a specified module's database file.
     * @return the database file.
     */
    fun getDatabaseFile(module: String): File {
      return File(Paths.get(getModulePath(module), "$module.db").toString())
    }
  }
}
