package modules.mx.logic

import api.logic.MXServer
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.*
import tornadofx.observableListOf
import kotlin.system.exitProcess

@DelicateCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
class MXCLI : IModule {
    override val moduleNameLong = "CLI"
    override val module = "CLI"

    override fun getIndexManager(): IIndexManager? {
        return null
    }

    /**
     * Runs the software without GUI in command line interpretation mode.
     */
    fun runCLI() {
        /**
         * Get config
         */
        log(MXLog.LogType.INFO, "Checking .ini file...")
        checkInstallation()
        /**
         * Check if user is logged in
         */
        val login: Boolean = if (activeUser.username.isEmpty()) {
            cliLogin()
        } else true
        var terminated = !login
        var inputArgs: List<String>
        while (!terminated) {
            print("\n${activeUser.username}:> "); inputArgs = (readLine() ?: "").split(" ")
            when (inputArgs[0]) {
                "exit", "close", "terminate" -> terminated = true
                "help" -> cliHelp(inputArgs)
                "start" -> cliStart(inputArgs)
                "load" -> cliLoad(inputArgs)
                "show" -> cliShow(inputArgs)
            }
        }
        cliExit()
    }

    private fun cliShow(args: List<String>) {
        if (args.size < 2) {
            log(MXLog.LogType.ERROR, "Not enough arguments.")
        } else {
            when (args[1]) {
                "dbstats" -> {
                    val header = arrayOf("DB", "Desc", "#", "DB KiB", "IX KiB", "Date", "User")
                    val ix = observableListOf(m1GlobalIndex, m2GlobalIndex, m3GlobalIndex, m4GlobalIndex)
                    val data = d2Array(ix.size, header.size)
                    for (index in 0 until ix.size) {
                        data[index][0] = ix[index].module
                        data[index][1] = ix[index].moduleNameLong
                        data[index][2] = ix[index].getLastUniqueID().toString()
                        data[index][3] = ix[index].dbSizeKiByte.toString()
                        data[index][4] = ix[index].ixSizeKiByte.toString()
                        data[index][5] = ix[index].lastChangeDateUTC
                        data[index][6] = ix[index].lastChangeUser
                    }
                    cliPrint(header, data)
                }
            }
        }
    }

    private fun cliStart(args: List<String>) {
        if (args.size < 2) {
            log(MXLog.LogType.ERROR, "Not enough arguments.")
        } else {
            when (args[1]) {
                "server" -> {
                    server = MXServer()
                    taskJobGlobal = MXTicker.startTicker()
                }
            }
        }
    }

    private fun cliExit() {
        exitMain()
        log(MXLog.LogType.INFO, "System terminated by user.")
        exitProcess(0)
    }

    private fun cliLoad(args: List<String>) {
        if (args.size < 2) {
            log(MXLog.LogType.ERROR, "Not enough arguments.")
        } else {
            if (args[1] == "index") {
                if (args.size < 3) {
                    loadIndex()
                } else {
                    loadIndex(args[2])
                }
            }
        }
    }

    /**
     * Shows useful information to the user.
     */
    private fun cliHelp(uArgs: List<String>) {
        val args = if (uArgs.size < 2) {
            listOf(uArgs[0], "")
        } else uArgs
        val help = "help {option} -> shows help"
        val exit =
            "exit/close/terminate -> terminates the software, shutting down the server and ticker task"
        //****************************************************
        //******************* HELP TEXTS *********************
        //****************************************************
        val chuser =
            "chuser -> change the current user by forcing a login"

        val load =
            "load [argument] -> loads [argument] into the system"
        val loadDetail = "$load\n" +
                "\tindex -> loads all available indices" +
                "\tindex {module} -> loads a specific index (e.g. load index m1)"

        val start =
            "start [argument] -> starts [argument]"
        val startDetail = "$start\n" +
                "\tserver -> starts the server"

        val show =
            "show [argument] -> shows info about [argument]"
        val showDetail = "$show\n" +
                "\tmodules -> shows all available modules\n" +
                "\tdbstats -> shows database stats"
        //****************************************************
        val helpText = when (args[1]) {
            "help" -> help
            "exit" -> exit
            "chuser" -> chuser
            "start" -> startDetail
            "load" -> loadDetail
            "show" -> showDetail
            else -> {
                exit +
                        "\n$chuser" +
                        "\n$start" +
                        "\n$load" +
                        "\n$show"
            }
        }
        println(helpText)
    }

    /**
     * Forces the user to log in by providing credentials.
     * @return true if the user is logged in now.
     */
    private fun cliLogin(): Boolean {
        var loggedIn = false
        while (!loggedIn) {
            var username = ""
            var password = ""
            log(MXLog.LogType.INFO, "Please log in by providing credentials.")
            log(MXLog.LogType.WARNING, "Careful: the password will be invisible while entering it.")
            while (username.isEmpty()) {
                print("CWO:> Username: ")
                username = readLine() ?: ""
            }
            while (password.isEmpty()) {
                print("CWO:> Password: ")
                password = System.console().readPassword().concatToString()
            }
            loggedIn = MXUserManager().login(username, password, doLog = true)
        }
        return loggedIn
    }

    /**
     * Prints an ASCII table of the provided contents.
     */
    private fun cliPrint(header: Array<String>, data: Array<Array<String>>) {
        val boundaries = IntArray(header.size)
        for (hd in header.indices) {
            boundaries[hd] = header[hd].length + 2
        }
        for (col in data.indices) {
            for (row in 0 until data[col].size) {
                if ((data[col][row].length + 2) > boundaries[row]) {
                    boundaries[row] = data[col][row].length + 2
                }
            }
        }
        println()
        var separator = ""
        for (col in boundaries.indices) {
            separator += "+"
            for (tmp in 0 until boundaries[col]) separator += "-"
            if (col == header.size - 1) separator += "+"
        }
        println(separator)
        var cell: String
        for (row in -1 until data.size) {
            for (col in boundaries.indices) {
                cell = if (row == -1) {
                    "| ${header[col]}"
                } else {
                    "| ${data[row][col]}"
                }
                print(cell)
                for (pad in 0..(boundaries[col] - cell.length)) print(" ")
                if (col == header.size - 1) print("|")
            }
            println()
            if (row == -1) println(separator)
        }
        println(separator)
    }
}
