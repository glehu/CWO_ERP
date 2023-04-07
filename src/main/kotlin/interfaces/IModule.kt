package interfaces

import api.misc.json.EntryBytesListJson
import api.misc.json.EntryListJson
import api.misc.json.QueryResult
import db.CwODB
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.Json
import modules.mx.getModulePath
import modules.mx.logic.Emailer
import modules.mx.logic.Log
import modules.mx.logic.indexFormat
import modules.mx.maxSearchResultsGlobal
import modules.mx.protoBufGlobal
import modules.mx.serverIPAddressGlobal
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Paths

@InternalAPI
@ExperimentalSerializationApi
interface IModule {
  val moduleNameLong: String
  val module: String

  fun getIndexManager(): IIndexManager?

  fun getServerUrl(): String {
    return "${serverIPAddressGlobal}/"
  }

  fun getApiUrl(): String {
    return "${getServerUrl()}api/${module.lowercase()}/"
  }

  /**
   * Saves the [IEntry] to the database.
   * @return the uID of the entry.
   */
  suspend fun save(
    entry: IEntry,
    raf: RandomAccessFile? = null,
    indexWriteToDisk: Boolean = true,
    userName: String = "",
    unlock: Boolean = true
  ): Long {
    val indexManager = getIndexManager()!!
    val rafLocal = raf ?: CwODB.openRandomFileAccess(module, CwODB.CwODB.RafMode.READWRITE)
    var posDB: Long = -1L
    var byteSize: Int = -1
    if (entry.uID != -1L) {
      val index = indexManager.getBaseIndex(entry.uID)
      if (index != null) {
        posDB = index.pos
        byteSize = index.byteSize
      }
    }
    entry.initialize()
    val (posDBXt, byteSizeXt) = CwODB.saveEntry(
            entryBytes = encode(entry), posDB = posDB, byteSize = byteSize, module = indexManager.module,
            raf = rafLocal)
    val posDBX: Long = posDBXt
    val byteSizeX: Int = byteSizeXt
    indexManager.indexEntry(entry, posDBX, byteSizeX, indexWriteToDisk, "")
    if (raf == null) CwODB.closeRandomFileAccess(rafLocal)
    val uID: Long = entry.uID
    //Unlock the entry
    if (unlock) getIndexManager()!!.setEntryLock(uID, false)
    return uID
  }

  /**
   * Used to retrieve an [IEntry] from the database using the provided uID.
   *
   * This function also locks the entry, making it uneditable for others. If this is not wanted,
   * use the [load] function instead.
   * If an [IEntry] of another module needs to be retrieved, call this function with the controller of that module.
   * @return an [IEntry] with the provided unique identifier.
   */
  @ExperimentalSerializationApi
  fun get(uID: Long): IEntry {
    return decode(getBytes(uID, true))
  }

  /**
   * Used to retrieve an [IEntry] from the database using the provided uID.
   *
   * This function doesn't lock the entry, which means it cannot be saved.
   * If an entry of another module needs to be retrieved, call this function with the controller of that module.
   * @return an [IEntry] with the provided unique identifier.
   */
  @ExperimentalSerializationApi
  fun load(uID: Long): IEntry {
    return decode(getBytes(uID, false))
  }

  /**
   * Used to retrieve a [ByteArray] of an [IEntry] from the database using the provided uID.
   * It is possible to retrieve an [IEntry] of another module if that module gets passed into the function.
   * @return the [ByteArray] of an [IEntry] with the provided unique identifier.
   */
  fun getBytes(
    uID: Long,
    lock: Boolean = false,
    userName: String = ""
  ): ByteArray {
    var entryBytes: ByteArray = byteArrayOf()
    if (uID != -1L) {
      entryBytes = CwODB.getEntryByteArrayFromUID(
              uID = uID, indexManager = getIndexManager()!!)
      // Lock the entry (if: GET)
      getIndexManager()!!.setEntryLock(uID, lock, userName)
    }
    return entryBytes
  }

  /**
   * Used to encode an IEntry into a [ByteArray]
   * @return the encoded entry as a [ByteArray].
   */
  fun encode(entry: IEntry): ByteArray {
    return protoBufGlobal.encodeToByteArray(entry)
  }

  /**
   * Used to decode an IEntry from a [ByteArray]
   * @return the decoded entry.
   * @throws Exception
   */
  fun decode(entryBytes: ByteArray): IEntry {
    try {
      return protoBufGlobal.decodeFromByteArray(entryBytes)
    } catch (e: Exception) {
      throw e
    }
  }

  /**
   * @return [EntryBytesListJson]
   */
  fun getEntryBytesListJson(
    searchText: String,
    ixNr: Int,
    exactSearch: Boolean = false
  ): EntryBytesListJson {
    val resultsListJson = EntryBytesListJson(0, arrayListOf())
    var resultCounter = 0
    CwODB.getEntriesFromSearchString(
            searchText = searchText.uppercase(), ixNr = ixNr, exactSearch = exactSearch,
            indexManager = getIndexManager()!!) { _, bytes ->
      resultCounter++
      resultsListJson.resultsList.add(bytes)
    }
    resultsListJson.total = resultCounter
    return resultsListJson
  }

  /**
   * @return [EntryListJson]
   */
  fun getEntryListJson(
    searchText: String,
    ixNr: Int,
    prettyPrint: Boolean = false,
    exactSearch: Boolean = false
  ): EntryListJson {
    val resultsListJson = EntryListJson(0, arrayListOf())
    var resultCounter = 0
    CwODB.getEntriesFromSearchString(
            searchText = searchText.uppercase(), ixNr = ixNr, exactSearch = exactSearch,
            indexManager = getIndexManager()!!) { _, bytes ->
      resultCounter++
      val entry = decode(bytes)
      entry.initialize()
      resultsListJson.resultsList.add(
              getIndexManager()!!.encodeToJsonString(
                      entry = entry, prettyPrint = prettyPrint))
    }
    resultsListJson.total = resultCounter
    return resultsListJson
  }

  /**
   * @return a list of indices [ArrayList]<[String]> the user can search in.
   */
  @ExperimentalSerializationApi
  fun getIndexUserSelection(): ArrayList<String> {
    return getIndexManager()!!.getIndicesList()
  }

  /**
   * Displays text on the console and writes it to the module's log file.
   */
  suspend fun log(
    type: Log.Type,
    text: String,
    apiEndpoint: String = "",
    moduleAlt: String? = null
  ) {
    Log.log(
            module = moduleAlt ?: module, type = type, text = text, caller = moduleNameLong, apiEndpoint = apiEndpoint)
  }

  fun getEntryLock(
    uID: Long,
    userName: String = ""
  ): Boolean {
    val content = getIndexManager()!!.getBaseIndex(uID)?.content ?: ""
    return (content.isNotEmpty() && content != userName)
  }

  fun setEntryLock(
    uID: Long,
    doLock: Boolean,
    userName: String = ""
  ): Boolean {
    var success = false
    val indexManager = getIndexManager()!!
    val entryLocked = indexManager.getEntryLock(uID, userName)
    if (doLock) {
      if (!entryLocked) {
        indexManager.getBaseIndex(uID)?.content = userName
        success = true
      }
    } else {
      // If the entry is locked by the user that is trying to unlock -> unlock
      if (indexManager.getBaseIndex(uID)?.content == userName) {
        indexManager.getBaseIndex(uID)?.content = ""
        success = true
      }
    }
    return success
  }

  /**
   * Gets a json serializer with the option of pretty printing.
   * @return a json serializer.
   */
  fun json(prettyPrint: Boolean): Json {
    return Json { this.prettyPrint = prettyPrint }
  }

  /**
   * @return the settings file of a provided module
   */
  fun getSettingsFileText(
    moduleShort: String = module,
    subSetting: String = "",
    check: Boolean = true
  ): String {
    return getSettingsFile(moduleShort = moduleShort, subSetting = subSetting).readText()
  }

  fun getSettingsFile(
    moduleShort: String = module,
    subSetting: String = "",
    check: Boolean = true
  ): File {
    if (check) checkSettingsFile(subSetting = subSetting)
    val sub = if (subSetting.isNotEmpty()) {
      "-$subSetting"
    } else ""
    return File(Paths.get(getModulePath(moduleShort), "$moduleShort$sub.ini").toString())
  }

  private fun checkSettingsFile(subSetting: String = ""): Boolean {
    var ok = false
    val settingsPath = File(getModulePath(module))
    if (!settingsPath.isDirectory) settingsPath.mkdirs()
    val settingsFile = getSettingsFile(subSetting = subSetting, check = false)
    if (!settingsFile.isFile) {
      ok = false
      settingsFile.createNewFile()
      if (settingsFile.isFile) ok = true
    }
    return ok
  }

  suspend fun sendEmail(
    subject: String,
    body: String,
    recipient: String
  ): Boolean {
    Emailer().sendEmailOverMailServer(subject, body, recipient)
    return true
  }

  /**
   * Retrieves, if present, entries that fit the provided search text.
   * The search can be done for a specific index, or using all available indices.
   * @return IEntry
   */
  fun getEntriesFromIndexSearch(
    searchText: String,
    ixNr: Int,
    showAll: Boolean,
    paginationIndex: Int = 0,
    pageSize: Int = maxSearchResultsGlobal,
    skip: Int = 0,
    format: Boolean = true,
    numberComparison: Boolean = false,
    entryOut: (IEntry) -> Unit
  ) {
    val searchTextFormatted = if (format) indexFormat(searchText) else searchText
    CwODB.getEntriesFromSearchString(
            searchText = searchTextFormatted, ixNr = ixNr, exactSearch = true, indexManager = getIndexManager()!!,
            maxSearchResults = if (showAll) -1 else pageSize, numberComparison = numberComparison,
            paginationIndex = paginationIndex, skip = skip) { _, bytes ->
      try {
        entryOut(decode(bytes))
      } catch (e: Exception) {
        runBlocking {
          log(Log.Type.ERROR, "IXLOOK-ERR-${e.message}")
        }
      }
    }
  }

  fun buildQueryRegexPattern(query: String): Pair<Regex, List<String>> {
    // Split words on each whitespace after removing duplicate whitespaces
    val cleanQuery = query.replace("\\s+".toRegex()) { it.value[0].toString() }
    val queryWords = cleanQuery.split("\\s".toRegex())
    val queryFormatted = StringBuilder()
    for ((amount, word) in queryWords.withIndex()) {
      if (amount > 0) queryFormatted.append("|")
      queryFormatted.append("(")
      queryFormatted.append(word)
      // Negative Lookahead to prevent tokens to be matched multiple times
      queryFormatted.append("(?!.*")
      queryFormatted.append(word)
      queryFormatted.append("))")
    }
    val queryFormattedString = queryFormatted.toString()
    return Pair(queryFormattedString.toRegex(RegexOption.IGNORE_CASE), queryWords)
  }

  fun applyQueryRegexPattern(
    regexPattern: Regex,
    queryWordsResults: MutableMap<String, Long>,
    entry: IEntry,
    title: String = "",
    keywords: String = "",
    description: String = "",
    authorUsername: String = "",
    filterOverride: String = ""
  ): QueryResult {
    var matchesAll: Sequence<MatchResult>
    var rating = 0
    var accuracy = 0
    var regexMatchCounts: Int
    var matchDestructuredList: List<String>
    // Title
    if (filterOverride.isEmpty() || filterOverride.contains("title")) {
      matchesAll = regexPattern.findAll(title)
      regexMatchCounts = matchesAll.count()
      if (regexMatchCounts > 0) {
        rating += 2
        accuracy += regexMatchCounts
        for (match in matchesAll) {
          matchDestructuredList = match.destructured.toList()
          for (matchedWord in matchDestructuredList) {
            if (matchedWord.isNotEmpty()) {
              queryWordsResults[matchedWord] = entry.uID
            }
          }
        }
      }
    }
    // Keywords
    if (filterOverride.isEmpty() || filterOverride.contains("keywords")) {
      matchesAll = regexPattern.findAll(keywords)
      regexMatchCounts = matchesAll.count()
      if (regexMatchCounts > 0) {
        rating += 2
        accuracy += regexMatchCounts
        for (match in matchesAll) {
          matchDestructuredList = match.destructured.toList()
          for (matchedWord in matchDestructuredList) {
            if (matchedWord.isNotEmpty()) {
              queryWordsResults[matchedWord] = entry.uID
            }
          }
        }
      }
    }
    // Description
    if (filterOverride.isEmpty() || filterOverride.contains("description")) {
      matchesAll = regexPattern.findAll(description)
      regexMatchCounts = matchesAll.count()
      if (regexMatchCounts > 0) {
        rating += 1
        accuracy += regexMatchCounts
        for (match in matchesAll) {
          matchDestructuredList = match.destructured.toList()
          for (matchedWord in matchDestructuredList) {
            if (matchedWord.isNotEmpty()) {
              queryWordsResults[matchedWord] = entry.uID
            }
          }
        }
      }
    }
    // Author
    if (filterOverride.isEmpty() || filterOverride.contains("author")) {
      matchesAll = regexPattern.findAll(authorUsername)
      regexMatchCounts = matchesAll.count()
      if (regexMatchCounts > 0) {
        rating += 1
        accuracy += regexMatchCounts
        for (match in matchesAll) {
          matchDestructuredList = match.destructured.toList()
          for (matchedWord in matchDestructuredList) {
            if (matchedWord.isNotEmpty()) {
              queryWordsResults[matchedWord] = entry.uID
            }
          }
        }
      }
    }
    // Calculate accuracy percentage
    var foundTmp = 0
    for (queryWordResult in queryWordsResults) {
      if (queryWordResult.value == entry.uID) {
        foundTmp += 1
      }
    }
    val accuracyPercentage = foundTmp.toDouble() / queryWordsResults.size.toDouble()
    // Return Result
    return QueryResult(entry, rating, accuracy, accuracyPercentage)
  }

  fun getQueryWordsResultInit(
    queryWords: List<String>
  ): MutableMap<String, Long> {
    val map = mutableMapOf<String, Long>()
    for (word in queryWords) {
      map[word] = -1
    }
    return map
  }

  fun sortQueryResults(results: ArrayList<QueryResult>): List<QueryResult> {
    // Negative values to sort in descending order! Nice "hack"!
    return results.sortedWith(compareBy({ -it.rating }, { -it.accuracyPercent }, { -it.accuracyTotalCount }))
  }
}
