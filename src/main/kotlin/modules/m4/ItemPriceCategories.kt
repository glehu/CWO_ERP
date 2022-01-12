package modules.m4

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import modules.m4.logic.ItemPriceManager

@InternalAPI
@ExperimentalSerializationApi
@Serializable
data class ItemPriceCategories(val type: ItemPriceManager.CategoryType) {
  val priceCategories = mutableMapOf<Int, ItemPriceCategory>()
}
