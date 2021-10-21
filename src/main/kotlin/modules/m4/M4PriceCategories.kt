package modules.m4

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import modules.m4.logic.M4PriceManager

@InternalAPI
@ExperimentalSerializationApi
@Serializable
data class M4PriceCategories(val type: M4PriceManager.CategoryType) {
    val priceCategories = mutableMapOf<Int, M4PriceCategory>()
}