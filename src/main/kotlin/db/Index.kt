package db

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Index(@SerialName("m") val module: String)
{
    @SerialName("i") val indexMap = mutableMapOf<Int, IndexContent>()
}