package modules.mx

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MXIni(
    @SerialName("encryption key") var token: String,
    @SerialName("data path") var dataPath: String,
    @SerialName("max search results") var maxSearchResults: Int,
    @SerialName("difference from utc in hours") var differenceFromUTC: Int
)