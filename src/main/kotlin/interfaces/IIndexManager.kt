package interfaces

import com.github.ajalt.mordant.animation.ProgressAnimation
import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.rendering.TextColors
import db.CwODB
import db.Index
import db.IndexContent
import io.ktor.util.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.LastChange
import modules.mx.getModulePath
import modules.mx.logic.Timestamp.Timestamp.convUnixHexToUnixTimestamp
import modules.mx.logic.Timestamp.Timestamp.getLocalTimestamp
import modules.mx.logic.Timestamp.Timestamp.getUTCTimestamp
import modules.mx.logic.Timestamp.Timestamp.getUnixTimestampHex
import modules.mx.logic.indexFormat
import modules.mx.logic.roundTo
import modules.mx.terminal
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.set
import kotlin.concurrent.thread

@InternalAPI
@ExperimentalSerializationApi
interface IIndexManager : IModule {
  /**
   * All [Index] entries managed by this [IIndexManager] instance.
   */
  val indexList: MutableMap<Int, Index>

  /**
   * The maximum amount of entries that this [IIndexManager] instance is allowed to contain.
   * If the threshold is surpassed, a new IIndexManager instance will be created.
   *
   * The capacity is specified using a [Long] but it is not allowed to be greater than [Int.MAX_VALUE].
   * This is because of the internal structure of [MutableMap].
   */
  var capacity: Long

  var isRemote: Boolean

  var remoteURL: String

  /**
   * Specifies the depth of this [IIndexManager] in the scope of all other instances.
   * Deeper instances manage [Index] entries that were added later in the timeline.
   */
  var level: Long

  /**
   * Self replicating [IIndexManager] instances if needed.
   */
  var prevManager: IIndexManager?

  /**
   * Self replicating [IIndexManager] instances if needed.
   */
  var nextManager: IIndexManager?

  /**
   * The last uID generated.
   */
  var lastUID: AtomicLong

  /**
   * Specifies the first uID that this [IIndexManager] instance can handle.
   * All other uIDs are being managed by other IIndexManager instances.
   */
  var localMinUID: Long

  /**
   * Specifies the last uID that this [IIndexManager] instance can handle.
   * All other uIDs are being managed by other IIndexManager instances.
   */
  var localMaxUID: Long

  // #### Statistical values below ####
  var lastChangeDateHex: String
  var lastChangeDateUTC: String
  var lastChangeDateLocal: String
  var lastChangeUser: String
  var dbSizeMiByte: Double
  var ixSizeMiByte: Double

  /**
   * This function needs to be called in the init{} block of the [IIndexManager].
   */
  fun initialize(vararg ixNumbers: Int) {
    if (level < 0) error("level of IIndexManager cannot be smaller than 0")
    terminal.println("${TextColors.gray("CWO :> IX")} Initializing $moduleNameLong @ level $level...")
    // Get or generate UID information
    lastUID = updateLastUID()
    getLastChangeDates()
    // Calculate the lowest possible uID
    // Example: 1
    localMinUID = capacity * level
    // Calculate the highest possible uID for this index manager.
    // We subtract one from the initial capacity (capacity - 1)
    // because we're working with indices that start with 0.
    localMaxUID = (capacity - 1) + localMinUID
    terminal.println("${TextColors.gray("CWO :> IX")} [ $localMinUID < uID > $localMaxUID ]")
    // Retrieve indices from disk
    val progress = terminal.progressAnimation {
      text("Retrieving indices...")
      progressBar(pendingChar = "-", completeChar = "|")
      percentage()
      completed()
    }
    terminal.info.updateTerminalSize()
    progress.start()
    progress.updateTotal(ixNumbers.size.toLong() + 1L)
    addIndex(0)
    progress.advance(1L)
    getIndicesFromArray(ixNumbers, progress)
    // Stop progress animation
    progress.stop()
    progress.clear()
    terminal.println("${TextColors.green("Success!")}\n")
    // Get statistical values
    getFileSizes()
    // Check for deeper index managers
    terminal.println("${TextColors.gray("CWO :> IX")} Looking for deeper index managers...")
    val ix1 = getIndexFile(0, level + 1L)
    if (ix1.isFile) {
      terminal.println("${TextColors.gray("CWO :> IX")} Deeper index manager found... initializing...")
      terminal.println("${TextColors.gray("CWO :> IX")} Retrieving from ${ix1.name}...")
      // Build and set up new IIndexManager
      val newManager = buildNewIndexManager()
      // Set references
      newManager.prevManager = this
      nextManager = newManager
    } else {
      terminal.println("${TextColors.gray("CWO :> IX")} None found!\n")
    }
  }

  fun getFileSizes() {
    dbSizeMiByte = ((CwODB.getDatabaseFile(module).length()).toDouble() / 1024.0) / 1024.0
    ixSizeMiByte = 0.0
    for (index in indexList.entries) {
      ixSizeMiByte += ((getIndexFile(index.key).length()).toDouble() / 1024.0) / 1024.0
    }
    dbSizeMiByte = dbSizeMiByte.roundTo(2)
    ixSizeMiByte = ixSizeMiByte.roundTo(2)
  }

  /**
   * @return a new unique identifier as an [AtomicLong].
   */
  fun getUID(): Long {
    val uID = lastUID.getAndIncrement()
    thread {
      setLastUniqueID(lastUID)
    }
    return uID
  }

  fun setLastChangeData(uID: Long, userName: String) {
    lastChangeDateHex = getUnixTimestampHex()
    val lastChange = LastChange(uID, lastChangeDateHex, userName)
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
    entry: IEntry, posDB: Long, byteSize: Int, writeToDisk: Boolean, userName: String
  )

  fun buildNewIndexManager(): IIndexManager

  /**
   * Used to build indices for an entry.
   */
  suspend fun buildIndices(
    uID: Long, posDB: Long, byteSize: Int, writeToDisk: Boolean, userName: String, vararg indices: Pair<Int, String>
  ) {
    if (uID != -1L && uID < localMinUID) {
      if (prevManager != null) {
        prevManager!!.buildIndices(uID, posDB, byteSize, writeToDisk, userName, *indices)
        return
      } else return // TODO: Check for unserialized index manager
    } else if (uID > localMaxUID) {
      if (nextManager != null) {
        nextManager!!.buildIndices(uID, posDB, byteSize, writeToDisk, userName, *indices)
        return
      } else {
        // Build and set up new IIndexManager
        val newManager = buildNewIndexManager()
        // Set references
        newManager.prevManager = this
        nextManager = newManager
        // Perform action
        newManager.buildIndices(uID, posDB, byteSize, writeToDisk, userName, *indices)
        return
      }
    }
    buildDefaultIndex(uID, posDB, byteSize)
    for ((ixNr, ixContent) in indices) {
      if (ixContent.isNotEmpty()) {
        if (indexList[ixNr] == null) addIndex(ixNr)
        synchronized(this) {
          indexList[ixNr]!!.indexMap[uID] = IndexContent(
                  content = indexFormat(ixContent).uppercase()
          )
        }
      } else {
        // Remove "empty" indices to save lots of space
        synchronized(this) {
          indexList[ixNr]!!.indexMap.remove(uID)
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
  private fun buildDefaultIndex(uID: Long, posDB: Long, byteSize: Int) {
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
   * Writes the index values stored in the RAM onto the disk.
   */
  suspend fun writeIndexData() = runBlocking {
    synchronized(this) {
      for (index in indexList.entries) {
        launch {
          getIndexFile(index.key).writeText(
                  Json.encodeToString(indexList[index.key])
          )
        }
      }
    }
  }

  fun getIndicesFromArray(ixNumbers: IntArray, progress: ProgressAnimation) {
    for (ixNr in ixNumbers) {
      addIndex(ixNr)
      progress.advance(1L)
    }
  }

  /**
   * @return an instance of Index to be used in IndexManagers
   */
  fun getIndex(ixNr: Int): Index {
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
  fun setLastChangeValues(lastChange: LastChange) {
    getLastChangeDateHexFile().writeText(Json.encodeToString(lastChange))
  }

  /**
   * @return the index [File] of a provided module
   */
  fun getIndexFile(ixNr: Int, levelOverride: Long? = null): File {
    return File(Paths.get(getModulePath(module), "$module-$ixNr-${levelOverride ?: level}.ix").toString())
  }

  /**
   * @return the [File] storing the last change date hex value
   */
  private fun getLastChangeDateHexFile(): File {
    return File(Paths.get(getModulePath(module), "lastchange.json").toString())
  }

  /**
   * @return the last change data
   */
  fun getLastChange(module: String): LastChange {
    val lastChangeFile = getLastChangeDateHexFile()
    val lastChange: LastChange
    if (!lastChangeFile.isFile) {
      lastChangeFile.createNewFile()
      lastChange = LastChange(-1, getUnixTimestampHex(), "")
      setLastChangeValues(lastChange)
    } else {
      lastChange = Json.decodeFromString(lastChangeFile.readText())
    }
    return lastChange
  }

  /**
   * Used to write the last unique identifier to the database.
   */
  fun setLastUniqueID(uniqueID: AtomicLong) {
    getNuFile().writeText(uniqueID.toString())
  }

  /**
   * @return the last unique identifier as an [AtomicLong]
   */
  fun getLastUniqueID(): AtomicLong {
    checkNuFile()
    val nuFile = getNuFile()
    val lastUniqueIDNumber: AtomicLong
    val lastUniqueIDString: String = nuFile.readText()
    lastUniqueIDNumber = if (lastUniqueIDString.isNotEmpty()) {
      AtomicLong((lastUniqueIDString).toLong())
    } else AtomicLong(0)
    return lastUniqueIDNumber
  }

  private fun getNuFile(): File {
    return File(Paths.get(getModulePath(module), "$module.nu").toString())
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
   * @return the main [IndexContent] (ix0) for an entry's provided uID.
   */
  fun getBaseIndex(uID: Long): IndexContent? {
    return if (uID != -1L && uID < localMinUID) {
      if (prevManager != null) {
        prevManager!!.getBaseIndex(uID)
      } else null
    } else if (uID > localMaxUID) {
      if (nextManager != null) {
        nextManager!!.getBaseIndex(uID)
      } else null
    } else {
      indexList[0]!!.indexMap[uID]!!
    }
  }

  fun encodeToJsonString(entry: IEntry, prettyPrint: Boolean = false): String
  fun filterStringValues(ixNr: Int, searchText: String): Set<Long> {
    var filteredMap: Set<Long> = setOf()
    if (nextManager != null) {
      filteredMap = filteredMap union (nextManager!!.filterStringValues(ixNr, searchText))
    }
    filteredMap = filteredMap union (indexList[ixNr]!!.indexMap.filterValues {
      it.content.contains(searchText.toRegex())
    }.keys.reversed())
    return filteredMap
  }

  fun filterDoubleValues(ixNr: Int, searchText: String): MutableSet<Long> {
    val filteredMap = mutableSetOf<Long>()
    if (nextManager != null) {
      filteredMap union (nextManager!!.filterDoubleValues(ixNr, searchText)).reversed()
    }
    filteredMap union (indexList[ixNr]!!.indexMap.filterValues {
      it.content.toDouble() >= searchText.toDouble()
    }.keys.reversed())
    return filteredMap
  }

  /**
   * @return the index results for a search text from all available indices.
   */
  @OptIn(FlowPreview::class)
  fun returnFromAllIndices(searchText: String, updateProgress: (Map<Long, IndexContent>) -> Unit) {
    runBlocking {
      if (nextManager != null) {
        searchInAllIndices(searchText).flatMapMerge { nextManager!!.searchInAllIndices(searchText) }
          .collect { indexResult: Map<Long, IndexContent> ->
            updateProgress(indexResult)
          }
      } else {
        searchInAllIndices(searchText).collect { indexResult: Map<Long, IndexContent> ->
          updateProgress(indexResult)
        }
      }
    }
  }

  /**
   * Used to search in all indices by starting an index search flow for each index manager.
   * @return the [Flow] of an index manager's index search.
   */
  private fun searchInAllIndices(searchText: String): Flow<Map<Long, IndexContent>> = flow {
    for (ixNr in 1 until indexList.size) {
      val results = indexList[ixNr]!!.indexMap.filterValues {
        it.content.contains(searchText.toRegex())
      }
      emit(results)
    }
  }
}
