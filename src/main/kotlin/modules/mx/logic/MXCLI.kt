package modules.mx.logic

import api.logic.MXServer
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.activeUser
import modules.mx.server
import modules.mx.taskJobGlobal
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
            print("\n${activeUser.username} :> "); inputArgs = (readLine() ?: "").split(" ")
            when (inputArgs[0]) {
                "exit", "close", "terminate" -> terminated = true
                "help" -> cliHelp(inputArgs)
                "start" -> cliStart(inputArgs)
                "load" -> cliLoad(inputArgs)
            }
        }
        cliExit()
    }

    private fun cliStart(args: List<String>) {
        if (args.size < 2) {
            log(MXLog.LogType.ERROR, "Not enough arguments.")
        } else {
            when (args[1]) {
                "server" -> server = MXServer()
                "ticker" -> taskJobGlobal = MXTicker.startTicker()
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
                "\tserver -> starts the server\n" +
                "\tticker -> starts the ticker task"

        val show =
            "show [argument] -> shows info about [argument]"
        val showDetail = "$show\n" +
                "modules -> shows all available modules"
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
        println("$helpText\n")
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
}
