package modules.m4stockposting.logic

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.itemStockPostingIndexManager

@ExperimentalSerializationApi
@InternalAPI
class ItemStockPostingController : IModule {
  override val moduleNameLong = "ItemStockPostingController"
  override val module = "M4SP"
  override fun getIndexManager(): IIndexManager {
    return itemStockPostingIndexManager!!
  }
}
