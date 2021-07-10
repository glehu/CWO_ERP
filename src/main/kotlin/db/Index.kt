package db

import kotlinx.serialization.Serializable

@Serializable
data class Index(val module: String)
{
    val indexMap = mutableMapOf<Int, IndexContent>()
}