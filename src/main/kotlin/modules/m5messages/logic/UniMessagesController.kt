package modules.m5messages.logic

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.uniMessagesIndexManager

@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
class UniMessagesController: IModule {
  override val moduleNameLong = "UniMessagesController"
  override val module = "M5MSG"
  override fun getIndexManager(): IIndexManager {
    return uniMessagesIndexManager!!
  }
}
