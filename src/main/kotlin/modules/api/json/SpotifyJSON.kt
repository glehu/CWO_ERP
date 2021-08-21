package modules.api.json

import interfaces.ITokenData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyAuthCallbackJson(
    @SerialName("access_token")
    override var accessToken: String = "?",
    @SerialName("token_type")
    override var tokenType: String = "?",
    override var scope: String = "?",
    @SerialName("expires_in")
    override var expiresIn: Int = 0,
    @SerialName("refresh_token")
    override var refreshToken: String = "",
    //Automatic
    override var generatedAtUnixTimestamp: Long = 0,
    override var expireUnixTimestamp: Long = 0
) : ITokenData

@Serializable
data class SpotifyUserProfileJson(
    var country: String = "?",
    var display_name: String = "?",
    var email: String = "?",
    var external_urls: Map<String, String> = mapOf("?" to "?"),
    var followers: Map<String, String?> = mapOf("?" to "?"),
    var href: String = "?",
    var id: String = "?",
    var product: String = "?",
    var type: String = "?",
    var uri: String = "?"
)

@Serializable
data class SpotifyAlbumListJson(
    val href: String = "?",
    @SerialName("items")
    val albums: List<SpotifyAlbumJson> = listOf(SpotifyAlbumJson())
)

@Serializable
data class SpotifyAlbumJson(
    @SerialName("album_group")
    val albumGroup: String = "?",
    @SerialName("album_type")
    val albumType: String = "?",
    val artists: List<SpotifyArtistJson> = listOf(SpotifyArtistJson()),
    @SerialName("available_markets")
    val availableMarkets: List<String> = listOf("?"),
    @SerialName("external_urls")
    val externalUrls: Map<String, String> = mapOf("?" to "?"),
    val href: String = "?",
    val id: String = "?",
    val name: String = "?",
    @SerialName("release_date")
    val releaseDate: String = "?",
    @SerialName("release_date_precision")
    val releaseDatePrecision: String = "?",
    val type: String = "?",
    val uri: String = "?"
)

@Serializable
data class SpotifyArtistJson(
    @SerialName("external_urls")
    val externalUrls: Map<String, String> = mapOf("?" to "?"),
    val href: String = "?",
    val id: String = "?",
    val name: String = "?",
    val type: String = "?",
    val uri: String = "?"
)