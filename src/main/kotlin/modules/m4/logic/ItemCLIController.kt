package modules.m4.logic

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.m4.misc.M4Ini
import modules.mx.m4GlobalIndex

@ExperimentalSerializationApi
@InternalAPI
class ItemCLIController : IModule {
    override val moduleNameLong = "M4CLIController"
    override val module = "M4"
    override fun getIndexManager(): IIndexManager {
        return m4GlobalIndex!!
    }

    fun getIni(): M4Ini {
        val iniTxt = getSettingsFileText()
        return if (iniTxt.isNotEmpty()) Json.decodeFromString(iniTxt) else M4Ini()
    }
}
