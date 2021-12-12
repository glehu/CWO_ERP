package api.logic

import api.misc.json.EntryJson
import db.CwODB
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.*
import modules.mx.logic.Log
import modules.mx.logic.Timestamp
import modules.mx.logic.indexFormat

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

    private lateinit var inputChannel: ByteReadChannel
    private lateinit var outputChannel: ByteWriteChannel

    fun startSession() = runBlocking {
        launch {
            inputChannel = socket.openReadChannel()
            outputChannel = socket.openWriteChannel(autoFlush = true)
            log(
                logType = Log.LogType.COM,
                text = "TELNET OPEN ${socket.remoteAddress}",
                apiEndpoint = "telnet ${socket.localAddress}"
            )
            while (connected) {
                //Wait for message
                val input = inputChannel.readUTF8Line(Int.MAX_VALUE)
                lastMessage = Timestamp.getUnixTimestamp()
                //Process message
                handleInput(input)
            }
        }
    }

    private suspend fun endSession(reason: String = "") {
        if (reason.isNotEmpty()) {
            outputChannel.writeStringUtf8("$reason\r\n")
        }
        log(
            logType = Log.LogType.SYS,
            text = "TELNET CLOSE REASON $reason"
        )
        connected = false
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
            Action.DISCONNECT -> endSession("Bye! (User Disconnected)")
            else -> return
        }
    }

    private fun indexSearch(args: List<String>) {
        // 0    1        2       3              4          5
        // IXS <MODULE> <IX_NR> <ALL/SPECIFIC> <NAME/UID> <SEARCH_TEXT>
        log(logType = Log.LogType.COM, text = args.toString(), apiEndpoint = "telnet ixs")
        val indexManager = when (args[1]) {
            "M1" -> m1GlobalIndex!!
            "M2" -> m2GlobalIndex!!
            "M3" -> m3GlobalIndex!!
            "M4" -> m4GlobalIndex!!
            "M4SP" -> m4StockPostingGlobalIndex!!
            else -> return
        }
        if (args[4] == "NAME") {
            var first = true
            CwODB.getEntriesFromSearchString(
                searchText = indexFormat(args[5]),
                ixNr = args[2].toInt(),
                exactSearch = (args[3] == "SPECIFIC"),
                indexManager = indexManager
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
