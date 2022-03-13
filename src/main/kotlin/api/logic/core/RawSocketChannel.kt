package api.logic.core

import api.misc.json.EntryJson
import db.CwODB
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.util.network.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.contactIndexManager
import modules.mx.discographyIndexManager
import modules.mx.invoiceIndexManager
import modules.mx.itemIndexManager
import modules.mx.itemStockPostingIndexManager
import modules.mx.logic.Log
import modules.mx.logic.Timestamp
import modules.mx.logic.indexFormat
import modules.mx.maxSearchResultsGlobal

@ExperimentalSerializationApi
@InternalAPI
class RawSocketChannel(
  private val socket: Socket,
) : IModule {
  override val moduleNameLong = "RawSocketChannel"
  override val module = "MX"
  override fun getIndexManager(): IIndexManager? {
    return null
  }

  private var connected = true
  private var lastMessage = Timestamp.getUnixTimestamp()
  private var remoteAddress: NetworkAddress? = null

  private lateinit var inputChannel: ByteReadChannel
  private lateinit var outputChannel: ByteWriteChannel

  fun startSession() = runBlocking {
    sessionGuard(
      launch {
        inputChannel = socket.openReadChannel()
        remoteAddress = socket.remoteAddress
        outputChannel = socket.openWriteChannel(autoFlush = true)
        outputChannel.writeStringUtf8("HEY\r\n")
        log(
          logType = Log.LogType.SYS,
          text = "RAW SOCKET $remoteAddress"
        )
        while (connected) {
          //Wait for message
          val input = inputChannel.readUTF8Line()
          lastMessage = Timestamp.getUnixTimestamp()
          //Process message
          handleInput(input)
        }
      }
    )
  }

  private suspend fun sessionGuard(job: Job) {
    var run = true
    while (run) {
      delay(5000)
      if ((Timestamp.getUnixTimestamp() - lastMessage) >= 20) {
        endSession("Disconnected due to inactivity.")
        run = false
        job.cancel()
      }
    }
  }

  private suspend fun endSession(reason: String = "") {
    if (reason.isNotEmpty()) {
      outputChannel.writeStringUtf8("$reason\r\n")
    }
    log(
      logType = Log.LogType.SYS,
      text = "TELNET CLOSE $remoteAddress REASON $reason"
    )
    connected = false
    inputChannel.cancel()
    outputChannel.close()
    socket.dispose()
  }

  private enum class Action {
    NOTHING, ECHO, INDEXSEARCH, DISCONNECT
  }

  private suspend fun handleInput(input: String?) {
    if (input == null) return
    val args = input.split(" ")
    when (getActionType(args)) {
      Action.ECHO -> echo(args)
      Action.INDEXSEARCH -> indexSearch(args)
      Action.DISCONNECT -> endSession("BYE")
      else -> return
    }
  }

  private fun indexSearch(args: List<String>) {
    // 0    1        2       3                     4          5
    // IXS <MODULE> <IX_NR> <ALL(FULL)/SPE(FULL)> <NAME/UID> <SEARCH_TEXT>
    log(logType = Log.LogType.COM, text = args.toString(), apiEndpoint = "telnet ixs")
    val indexManager = when (args[1]) {
      "M1" -> discographyIndexManager!!
      "M2" -> contactIndexManager!!
      "M3" -> invoiceIndexManager!!
      "M4" -> itemIndexManager!!
      "M4SP" -> itemStockPostingIndexManager!!
      else -> return
    }
    if (args[4] == "NAME" || args[4] == "NMBR") {
      var first = true
      val maxSearchResultsOverride =
        if ((args[3].length == 7) && (args[3].substring(3, 7) == "FULL")) -1 else maxSearchResultsGlobal
      val numberComparison = (args[4] == "NMBR")
      CwODB.getEntriesFromSearchString(
        searchText = indexFormat(args[5]),
        ixNr = args[2].toInt(),
        exactSearch = (args[3].substring(0, 3) == "SPE"),
        maxSearchResults = maxSearchResultsOverride,
        indexManager = indexManager,
        numberComparison = numberComparison
      ) { _, bytes ->
        try {
          runBlocking {
            if (first) {
              outputChannel.writeStringUtf8("RESULTS\r\n")
              first = false
            }
            val entry = indexManager.decode(bytes)
            val entryJson = EntryJson(entry.uID, bytes)
            val jsonBody = Json.encodeToString(entryJson)
            outputChannel.writeStringUtf8("$jsonBody\r\n")
          }
        } catch (e: Exception) {
          log(Log.LogType.ERROR, "IXLOOK-ERR-${e.message}", "telnet ixs")
        }
      }
    } else {
      //TODO
    }
    runBlocking {
      endSession("DONE")
    }
  }

  private suspend fun echo(args: List<String>) {
    outputChannel.writeStringUtf8(args.drop(1).toString() + "\r\n")
  }

  private fun getActionType(args: List<String>): Action {
    return when (args[0].uppercase()) {
      "ECHO" -> Action.ECHO
      "IXS" -> Action.INDEXSEARCH
      "BYE" -> Action.DISCONNECT
      else -> Action.NOTHING
    }
  }
}
