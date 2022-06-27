package modules.mx.logic

import com.sultanofcardio.models.Email
import com.sultanofcardio.models.MailServer
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.mx.Ini
import modules.mx.getIniFile

@InternalAPI
@ExperimentalSerializationApi
class Emailer : IModule {
  override val moduleNameLong = "Emailer"
  override val module = "MX"
  override fun getIndexManager(): IIndexManager? {
    return null
  }

  private val jsonSerializer = Json {
    prettyPrint = true
  }
  private val iniVal: Ini = jsonSerializer.decodeFromString(getIniFile().readText())
  private val mailServer: MailServer

  init {
    mailServer = MailServer(
      host = iniVal.emailHost,
      port = iniVal.emailPort,
      username = iniVal.emailUsername,
      password = iniVal.emailPassword
    )
  }

  suspend fun sendEmailOverMailServer(
    subject: String,
    body: String,
    recipient: String
  ) {
    mailServer.sendEmail(
      Email(
        from = iniVal.emailAddress,
        subject = subject,
        body = body,
        recipient = recipient
      )
    )
    log(Log.Type.COM, "Email $subject sent to $recipient")
  }
}
