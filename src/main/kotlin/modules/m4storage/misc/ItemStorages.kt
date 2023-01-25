package modules.m4storage.misc

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import modules.m4storage.ItemStorage
import modules.m4storage.logic.ItemStorageManager

@InternalAPI
@ExperimentalSerializationApi
@Serializable
data class ItemStorages(val type: ItemStorageManager.CategoryType) {
  val storages = mutableMapOf<Long, ItemStorage>()
}
