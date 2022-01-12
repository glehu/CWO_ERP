package modules.m4

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import modules.m4.logic.ItemStorageManager

@InternalAPI
@ExperimentalSerializationApi
@Serializable
data class ItemStorages(val type: ItemStorageManager.CategoryType) {
  val storages = mutableMapOf<Int, ItemStorage>()
}
