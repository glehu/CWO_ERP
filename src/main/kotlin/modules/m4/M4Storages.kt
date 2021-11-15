package modules.m4

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import modules.m4.logic.M4StorageManager

@InternalAPI
@ExperimentalSerializationApi
@Serializable
data class M4Storages(val type: M4StorageManager.CategoryType) {
    val storages = mutableMapOf<Int, M4Storage>()
}
