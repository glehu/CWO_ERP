package db

import kotlinx.serialization.Serializable

@Serializable
data class IndexContent(val content: String, var pos: Long, var byteSize: Int)