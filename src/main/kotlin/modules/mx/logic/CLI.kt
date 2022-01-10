package modules.mx.logic

import api.logic.Server
import api.logic.TelnetServer
import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.rendering.TextColors.*
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import javafx.collections.ObservableList
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.*
import tornadofx.observableListOf
import kotlin.system.exitProcess

@DelicateCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
class CLI : IModule {
    override val moduleNameLong = "CLI"
    override val module = "MX"

    override fun getIndexManager(): IIndexManager? {
        return null
    }

    /**
     * Runs the software without GUI in command line interpretation (CLI) mode.
     */
    fun runCLI() {
        cliMode = true
        cliCheckIni()
        val login: Boolean = if (activeUser.username.isEmpty()) cliLogin() else true
        var terminated = !login
        var inputArgs: List<String>
        while (!terminated) {
            terminal.print("\n${gray("${activeUser.username}:>")} ")
            //Wait for user input
            inputArgs = (readLine() ?: "").split(" ")
            terminated = cliHandleInput(inputArgs)
        }
        cliExit()
    }

    private fun cliCheckIni() {
        val progress = terminal.progressAnimation {
            text("Checking .ini file...")
            progressBar(pendingChar = "-", completeChar = "|")
            percentage()
            completed()
        }
        terminal.info.updateTerminalSize()
        progress.start()
        progress.updateTotal(1L); Thread.sleep(100)
        //Perform action
        checkInstallation()
        progress.advance(1L); Thread.sleep(100)
        Thread.sleep(100); progress.stop()
        progress.clear()
        terminal.println("\n${green("Success!")}")
    }

    /**
     * Handles the user input.
     * @return true if the software needs to terminate.
     */
    private fun cliHandleInput(inputArgs: List<String>): Boolean {
        var terminated = false
        when (inputArgs[0]) {
            "exit", "close", "terminate" -> terminated = true
            "chuser" -> {
                activeUser = User("", "")
                cliLogin()
            }
            "help" -> cliHelp(inputArgs)
            "start" -> cliStart(inputArgs)
            "load" -> cliLoad(inputArgs)
            "show" -> cliShow(inputArgs)
            "qs" -> cliQuickStart()
        }
        return terminated
    }

    private fun cliQuickStart() {
        runBlocking {
            launch {
                loadIndex()
            }
        }
        server = Server()
        telnetServer = TelnetServer()
    }

    private fun cliShow(args: List<String>) {
        if (args.size < 2) {
            cliErrorNotEnoughArguments(args)
        } else {
            when (args[1]) {
                "dbstats" -> {
                    val header = arrayOf("DB", "Desc", "#", "DB MiB", "IX MiB", "Date", "User")
                    val ix = observableListOf(
                        //M1
                        discographyIndexManager,
                        //M2
                        contactIndexManager,
                        //M3
                        invoiceIndexManager,
                        //M4
                        itemIndexManager,
                        //M4SP
                        itemStockPostingIndexManager
                    )
                    val data = d2Array(ix.size, header.size)
                    for (i in 0 until ix.size) {
                        if (ix[i] != null) {
                            data[i][0] = ix[i]!!.module
                            data[i][1] = ix[i]!!.moduleNameLong
                            data[i][2] = ix[i]!!.getLastUniqueID().toString()
                            data[i][3] = ix[i]!!.dbSizeMiByte.toString()
                            data[i][4] = ix[i]!!.ixSizeMiByte.toString()
                            data[i][5] = ix[i]!!.lastChangeDateUTC
                            data[i][6] = ix[i]!!.lastChangeUser
                        } else {
                            data[i][0] = "<Module>"
                            data[i][1] = "<Not initialized>"
                        }
                    }
                    cliPrintTable(header, data)
                }
                "users" -> {
                    val userManager = UserManager()
                    val users: ObservableList<User> = if (args.size > 2 && args[2] == "-active") {
                        userManager.getActiveUsers()
                    } else {
                        userManager.getUsersObservableList(
                            users = observableListOf(User("", "")),
                            credentials = userManager.getCredentials()
                        )
                    }
                    userManager.getActiveUsers()
                    val header = arrayOf("User", "rM1", "rM2", "rM3", "rM4", "Online since")
                    val data = d2Array(users.size, header.size)
                    for (i in 0 until users.size) {
                        data[i][0] = users[i].username
                        data[i][1] = users[i].canAccessDiscography.toString()
                        data[i][2] = users[i].canAccessContacts.toString()
                        data[i][3] = users[i].canAccessInvoices.toString()
                        data[i][4] = users[i].canAccessInventory.toString()
                        data[i][5] = users[i].onlineSince
                    }
                    cliPrintTable(header, data)
                }
                "config" -> print(getIniFile().readText())
            }
        }
    }

    private fun cliStart(args: List<String>) {
        if (args.size < 2) {
            cliErrorNotEnoughArguments(args)
        } else {
            when (args[1]) {
                "server" -> server = Server()
                "telnet" -> telnetServer = TelnetServer()
            }
        }
    }

    private fun cliErrorNotEnoughArguments(args: List<String>) {
        terminal.println(
            red("ERROR: Not enough arguments! Type ${white("help ${args[0]}")} for a list of arguments.")
        )
    }

    private fun cliExit() {
        exitMain()
        log(Log.LogType.INFO, "System terminated by user.")
        terminal.println(
            "${gray("CWO:>")} ${green("System successfully terminated by user.")}"
        )
        exitProcess(0)
    }

    private fun cliLoad(args: List<String>) {
        if (args.size < 2) {
            cliErrorNotEnoughArguments(args)
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
            "exit/close/terminate -> terminates the software and shuts down the server if it is running."
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
                "\tserver -> starts the server\n" +
                "\ttelnet -> starts the telnet server"

        val show =
            "show [argument] -> shows info about [argument]"
        val showDetail = "$show\n" +
                "\tconfig  -> shows the config data\n" +
                "\tmodules -> shows all available modules\n" +
                "\tdbstats -> shows database stats\n" +
                "\tusers {flag} -> shows a list of users\n" +
                "\t\t-active -> shows all active users"
        val qs =
            "qs -> quick start. loads the indices and starts the servers."
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
                        "\n$show" +
                        "\n$qs"
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
            terminal.println(
                "${gray("CWO:>")} Please log in by providing credentials."
            )
            terminal.println(
                "${gray("CWO:>")} ${red("Careful: the password will be invisible while entering it.")}"
            )
            while (username.isEmpty()) {
                terminal.print("${gray("CWO:>")} Username: ")
                username = readLine() ?: ""
            }
            while (password.isEmpty()) {
                print("${gray("CWO:>")} Password: ")
                password = System.console().readPassword().concatToString()
            }
            loggedIn = UserManager().login(username, password, doLog = true)
        }
        return true
    }

    /**
     * Prints an ASCII table of the provided contents.
     */
    private fun cliPrintTable(header: Array<String>, data: Array<Array<String>>) {
        val boundaries = IntArray(header.size)
        for (hd in header.indices) boundaries[hd] = header[hd].length + 2
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
