package modules.mx.logic

import api.logic.core.Server
import api.logic.core.TelnetServer
import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.magenta
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.white
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import javafx.collections.ObservableList
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.User
import modules.mx.activeUser
import modules.mx.cliMode
import modules.mx.contactIndexManager
import modules.mx.discographyIndexManager
import modules.mx.getIniFile
import modules.mx.invoiceIndexManager
import modules.mx.itemIndexManager
import modules.mx.itemStockPostingIndexManager
import modules.mx.programPath
import modules.mx.server
import modules.mx.telnetServer
import modules.mx.terminal
import tornadofx.observableListOf
import kotlin.system.exitProcess

@ExperimentalCoroutinesApi
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
  suspend fun runCLI(args: Array<String>) {
    cliClearTerminal()
    cliMode = true
    cliCheckIni()
    log(Log.LogType.INFO, "BOOTING CWO ERP CLI MODE")
    if (args.contains("-env")) {
      log(Log.LogType.SYS, "LOADING ENVIRONMENT VARIABLES")
      // Get User and Password from the system's environment variables
      val envUser: String = System.getenv("CWOERPUSER") ?: "?"
      val envPass: String = System.getenv("CWOERPPASS") ?: "?"
      if (envUser != "?" && envPass != "?") {
        // Attempt to log in the user
        log(Log.LogType.SYS, "LOGGING IN")
        if (!UserCLIManager().login(envUser, envPass, doLog = true)) {
          log(Log.LogType.ERROR, "TERMINATED PROCESS REASON wrong-credentials")
          cliExit()
        }
      } else {
        log(
          Log.LogType.WARNING,
          "REMOVED FLAG -env REASON incomplete-env-variables HELP check CWOERPUSER and CWOERPPASS"
        )
      }
    }
    if (args.contains("-server")) {
      log(Log.LogType.SYS, "STARTING SERVER MODE")
      try {
        cliQuickStart()
        val terminated = false
        while (!terminated) {
          delay(1000)
        }
      } catch (e: Exception) {
        log(Log.LogType.ERROR, "ERROR WHILE STARTING SERVER MODE REASON ${e.message}")
        terminal.println(
          red("ERROR WHILE STARTING SERVER MODE REASON ${e.message}")
        )
        cliExit(false)
      }
    } else {
      // Prompts the user to log in if there is no active user
      val login: Boolean = if (activeUser.username.isEmpty()) cliLogin() else true
      cliClearTerminal()
      // Terminate if the user is somehow not logged in at this point
      var terminated = !login
      while (!terminated) {
        terminal.print("\n${magenta("${activeUser.username}:>")} ")
        //Wait for user input
        terminated = cliHandleInput((readLine() ?: "").split(" "))
      }
      cliExit()
    }
  }

  /**
   * Clears the terminal screen and resets the cursor position.
   */
  private fun cliClearTerminal() {
    terminal.cursor.move {
      clearScreen()
      setPosition(0, 0)
    }
  }

  private fun cliCheckIni() {
    val progress = terminal.progressAnimation {
      text("Checking for .ini file in $programPath...")
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
      "exit" -> terminated = true
      "chuser" -> {
        activeUser = User("", "")
        cliLogin()
      }
      "help" -> cliHelp(inputArgs)
      "start" -> cliStart(inputArgs)
      "load" -> cliLoad(inputArgs)
      "show" -> cliShow(inputArgs)
      "qs" -> cliQuickStart()
      "clear" -> cliClearTerminal()
      else -> terminal.println(red("ERROR: Unknown Command ${white(inputArgs[0])}"))
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
    if (args.size < 2 || args[1].isEmpty()) {
      cliErrorNotEnoughArguments(args)
    } else {
      when (args[1]) {
        "dbstat" -> {
          val header = arrayOf("DB", "Desc", "#", "DB MiB", "IX MiB", "Date", "User")
          val ix = listOf(
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
          for (i in ix.indices) {
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
              credentials = UserCLIManager().getCredentials()
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
    if (args.size < 2 || args[1].isEmpty()) {
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

  private fun cliExit(clearTerminal: Boolean = true) {
    if (clearTerminal) cliClearTerminal()
    log(Log.LogType.SYS, "Terminating System...")
    exitMain()
    terminal.println(
      "${gray("CWO:>")} ${green("System successfully terminated.")}"
    )
    exitProcess(0)
  }

  private fun cliLoad(args: List<String>) {
    if (args.size < 2 || args[1].isEmpty()) {
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
    val args = if (uArgs.size < 2 || uArgs[1].isEmpty()) {
      listOf(uArgs[0], "")
    } else uArgs
    val help = "help ${gray("{option}")} -> shows help"
    val exit =
      "exit -> terminates the software and shuts down the servers if they are running."
    //****************************************************
    //******************* HELP TEXTS *********************
    //****************************************************
    val chuser =
      "chuser -> change the current user by forcing a login"

    val load =
      "load ${gray("[argument]")} -> loads ${gray("[argument]")} into the system"
    val loadDetail = "$load\n" +
            "\tindex -> loads all available indices\n" +
            "\tindex ${gray("{module}")} -> loads a specific index (e.g. load index m1)"

    val start =
      "start ${gray("[argument]")} -> starts ${gray("[argument]")}"
    val startDetail = "$start\n" +
            "\tserver -> starts the server\n" +
            "\ttelnet -> starts the telnet server"

    val show =
      "show ${gray("[argument]")} -> shows info about ${gray("[argument]")}"
    val showDetail = "$show\n" +
            "\tconfig  -> shows the config data\n" +
            "\tmodules -> shows all available modules\n" +
            "\tdbstat -> shows database statistics\n" +
            "\tusers ${gray("{flag}")} -> shows a list of users\n" +
            "\t\t${gray("-active")} -> shows all active users"
    val qs =
      "qs -> quick start. loads the indices and starts the servers."
    val clear =
      "clear -> clears the terminal screen"
    //****************************************************
    val helpText = when (args[1]) {
      "help" -> help
      "exit" -> exit
      "chuser" -> chuser
      "start" -> startDetail
      "load" -> loadDetail
      "show" -> showDetail
      "clear" -> clear
      else -> {
        exit +
                "\n$chuser" +
                "\n$start" +
                "\n$load" +
                "\n$show" +
                "\n$qs" +
                "\n$clear"
      }
    }
    terminal.println(helpText)
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
      loggedIn = UserCLIManager().login(username, password, doLog = true)
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
