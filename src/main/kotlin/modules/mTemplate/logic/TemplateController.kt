package modules.mTemplate.logic

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
class TemplateController : IModule {
  override val moduleNameLong = "Template"
  override val module = "M0TEMPLATE"
  override fun getIndexManager(): IIndexManager {
    return uniMessagesIndexManager!!
  }
}
