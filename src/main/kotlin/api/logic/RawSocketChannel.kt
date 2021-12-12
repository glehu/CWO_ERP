package api.logic

import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import modules.mx.logic.Timestamp
import kotlin.math.absoluteValue

class RawSocketChannel(private val socket: Socket, private val timeoutAfterSeconds: Long = 100) {
    private var connected = true
    private var lastMessage = Timestamp.getUnixTimestamp()

    private lateinit var inputChannel: ByteReadChannel
    private lateinit var outputChannel: ByteWriteChannel

    fun startSession() = runBlocking {
        sessionGuard(
            launch {
                inputChannel = socket.openReadChannel()
                outputChannel = socket.openWriteChannel(autoFlush = true)
                outputChannel.writeStringUtf8("--- CWO TELNET API --- Connection Established >:3\r\n")
                while (connected) {
                    //Wait for message
                    val input = inputChannel.readUTF8Line(Int.MAX_VALUE)
                    lastMessage = Timestamp.getUnixTimestamp()
                    //Process message
                    handleInput(input, outputChannel)
                }
            }
        )
    }

    private suspend fun sessionGuard(job: Job) {
        var run = true
        while (run) {
            delay(1000)
            if ((lastMessage - Timestamp.getUnixTimestamp()).absoluteValue < timeoutAfterSeconds) {
                endSession("Disconnected due to inactivity.")
                run = false
                job.cancel()
            }
        }
    }

    private suspend fun endSession(reason: String = "") {
        if (reason.isNotEmpty()) {
            outputChannel.writeStringUtf8("\r\n$reason\r\n")
        }
        socket.dispose()
    }

    private enum class Action {
        NOTHING, ECHO, DISCONNECT
    }

    private suspend fun handleInput(input: String?, outputChannel: ByteWriteChannel) {
        if (input == null) return
        val args = input.split(" ")
        when (getActionType(args)) {
            Action.ECHO -> echo(args, outputChannel)
            Action.DISCONNECT -> endSession("Bye! (User Disconnected)")
            else -> return
        }
    }

    private suspend fun echo(args: List<String>, outputChannel: ByteWriteChannel) {
        outputChannel.writeStringUtf8(args.drop(1).toString() + "\r\n")
    }

    private fun getActionType(args: List<String>): Action {
        return when (args[0].uppercase()) {
            "ECHO" -> Action.ECHO
            "BYE" -> Action.DISCONNECT
            else -> Action.NOTHING
        }
    }
}
