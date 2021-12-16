package modules.m4.logic

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import modules.m4.misc.ItemIni
import modules.mx.itemIndexManager

@ExperimentalSerializationApi
@InternalAPI
class ItemCLIController : IModule {
    override val moduleNameLong = "ItemCLIController"
    override val module = "M4"
    override fun getIndexManager(): IIndexManager {
        return itemIndexManager!!
    }

    fun getIni(): ItemIni {
        val iniTxt = getSettingsFileText()
        return if (iniTxt.isNotEmpty()) Json.decodeFromString(iniTxt) else ItemIni()
    }
}
