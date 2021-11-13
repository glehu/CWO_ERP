package modules.mx.logic

import com.sultanofcardio.models.Email
import com.sultanofcardio.models.MailServer
import interfaces.IIndexManager
import interfaces.IModule
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.mx.MXIni
import modules.mx.getIniFile

@ExperimentalSerializationApi
class MXEMailer : IModule {
    override val moduleNameLong = "MXEMailer"
    override val module = "MX"
    override fun getIndexManager(): IIndexManager? {
        return null
    }

    private val jsonSerializer = Json {
        prettyPrint = true
    }
    private val iniVal: MXIni = jsonSerializer.decodeFromString(getIniFile().readText())
    private val mailServer: MailServer

    init {
        mailServer = MailServer(
            host = iniVal.emailHost,
            port = iniVal.emailPort,
            username = iniVal.emailUsername,
            password = iniVal.emailPassword
        )
        log(MXLog.LogType.SYS, "EMail Server ${iniVal.emailUsername} created.")
    }

    fun sendEMailOverMailServer(
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
        log(MXLog.LogType.COM, "EMail <$subject> sent to <$recipient>")
    }
}
