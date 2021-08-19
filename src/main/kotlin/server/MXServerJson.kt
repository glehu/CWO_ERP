package server

import kotlinx.serialization.Serializable
import modules.interfaces.ITokenData

@Serializable
data class SpotifyAuthCallbackJson(
    override var access_token: String,
    override var token_type: String,
    override var scope: String,
    override var expires_in: Int,
    override var refresh_token: String = "",
    //Automatic
    override var generatedAtUnixTimestamp: Long = 0,
    override var expireUnixTimestamp: Long = 0
) : ITokenData

@Serializable
data class SpotifyUserProfileJson(
    val country: String,
    val display_name: String,
    val email: String,
    val external_urls: Map<String, String>,
    val followers: Map<String,String?>,
    val href: String,
    val id: String,
    val product: String,
    val type: String,
    val uri: String
)