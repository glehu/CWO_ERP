package modules.m5.logic

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.uniChatroomIndexManager

@ExperimentalSerializationApi
@InternalAPI
class UniChatroomController : IModule {
  override val moduleNameLong = "UniChatroomController"
  override val module = "M5"
  override fun getIndexManager(): IIndexManager {
    return uniChatroomIndexManager!!
  }
}
