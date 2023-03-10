package modules.mx.logic

import api.logic.core.Server
import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.programPath
import modules.mx.server
import modules.mx.terminal
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
    cliCheckIni()
    log(Log.Type.INFO, "BOOTING CWO ERP CLI MODE")
    if (args.contains("-env")) {
      log(Log.Type.SYS, "LOADING ENVIRONMENT VARIABLES")
      // Get User and Password from the system's environment variables
      val envUser: String = System.getenv("CWOERPUSER") ?: "?"
      val envPass: String = System.getenv("CWOERPPASS") ?: "?"
      if (envUser != "?" && envPass != "?") {
        // Attempt to log in the user
        log(Log.Type.SYS, "LOGGING IN")/*
        if (!UserCLIManager().login(envUser, envPass, doLog = true)) {
          log(Log.Type.ERROR, "TERMINATED PROCESS REASON wrong-credentials")
          cliExit()
        }
        */
      } else {
        log(
                Log.Type.WARNING,
                "REMOVED FLAG -env REASON incomplete-env-variables HELP check CWOERPUSER and CWOERPPASS")
      }
    }
    if (args.contains("-server")) {
      log(Log.Type.SYS, "STARTING SERVER MODE")
      try {
        cliQuickStart()
        val terminated = false
        while (!terminated) {
          delay(1000)
        }
      } catch (e: Exception) {
        log(Log.Type.ERROR, "ERROR WHILE STARTING SERVER MODE REASON ${e.message}")
        terminal.println(
                red("ERROR WHILE STARTING SERVER MODE REASON ${e.message}"))
        cliExit(false)
      }
    } else {/*
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
       */
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

  private suspend fun cliCheckIni() {
    val progress = terminal.progressAnimation {
      text("Checking for .ini file in $programPath...")
      progressBar(pendingChar = "-", completeChar = "|")
      percentage()
      completed()
    }
    terminal.info.updateTerminalSize()
    progress.start()
    progress.updateTotal(1L); withContext(Dispatchers.IO) {
      Thread.sleep(100)
    }
    //Perform action
    checkInstallation()
    progress.advance(1L); withContext(Dispatchers.IO) {
      Thread.sleep(100)
    }
    withContext(Dispatchers.IO) {
      Thread.sleep(100)
    }; progress.stop()
    progress.clear()
    terminal.println("\n${green("Success!")}")
  }

  private fun cliQuickStart() {
    runBlocking {
      launch {
        loadIndex()
      }
    }
    server = Server()
  }

  private suspend fun cliExit(clearTerminal: Boolean = true) {
    if (clearTerminal) cliClearTerminal()
    log(Log.Type.SYS, "Terminating System...")
    exitMain()
    terminal.println(
            "${gray("CWO:>")} ${green("System successfully terminated.")}")
    exitProcess(0)
  }

}
