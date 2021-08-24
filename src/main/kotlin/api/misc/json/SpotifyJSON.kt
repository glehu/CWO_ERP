package api.misc.json

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
    var href: String = "?",
    @SerialName("items")
    var albums: List<SpotifyAlbumJson> = listOf(SpotifyAlbumJson()),
    var limit: Int = 0,
    var next: String? = "?",
    var offset: Int = 0,
    var total: Int = 0
)

@Serializable
data class SpotifyAlbumJson(
    @SerialName("album_group")
    var albumGroup: String = "?",
    @SerialName("album_type")
    var albumType: String = "?",
    var artists: List<SpotifyArtistJson> = listOf(SpotifyArtistJson()),
    @SerialName("available_markets")
    var availableMarkets: List<String> = listOf("?"),
    @SerialName("external_urls")
    var externalUrls: Map<String, String> = mapOf("?" to "?"),
    var href: String = "?",
    var id: String = "?",
    var name: String = "?",
    @SerialName("release_date")
    var releaseDate: String = "?",
    @SerialName("release_date_precision")
    var releaseDatePrecision: String = "?",
    var type: String = "?",
    var uri: String = "?"
)

@Serializable
data class SpotifyArtistListJson(
    var artists: List<SpotifyArtistJson> = listOf(SpotifyArtistJson())
)

@Serializable
data class SpotifyArtistJson(
    @SerialName("external_urls")
    var externalUrls: Map<String, String> = mapOf("?" to "?"),
    var followers: Map<String, String?> = mapOf("?" to "?"),
    var genres: List<String> = listOf("?"),
    var href: String = "?",
    var id: String = "?",
    var name: String = "?",
    var popularity: Int = 0,
    var type: String = "?",
    var uri: String = "?"
)

@Serializable
data class SpotifyTracklistJson(
    var href: String = "?",
    @SerialName("items")
    var tracks: List<SpotifyTrackJson> = listOf(SpotifyTrackJson()),
    var limit: Int = 0,
    var next: String? = "?",
    var offset: Int = 0,
    var total: Int = 0
)

@Serializable
data class SpotifyTrackJson(
    var artists: List<SpotifyArtistJson> = listOf(SpotifyArtistJson()),
    @SerialName("available_markets")
    var availableMarkets: List<String> = listOf("?"),
    @SerialName("disc_number")
    var discNumber: Int = 0,
    @SerialName("duration_ms")
    var durationMs: Int = 0,
    var explicit: Boolean = false,
    @SerialName("external_urls")
    var externalUrls: Map<String, String> = mapOf("?" to "?"),
    var href: String = "?",
    var id: String = "?",
    var name: String = "?",
    @SerialName("track_number")
    var trackNumber: Int = 0,
    var type: String = "?",
    var uri: String = "?"
)