package server

import kotlinx.serialization.Serializable
import modules.interfaces.ITokenData

@Serializable
data class SpotifyAuthCallbackJson(
    override val access_token: String,
    override val token_type: String,
    override val scope: String,
    override val expires_in: Int,
    override val refresh_token: String,
    //Automatic
    override var generatedAtUnixTimestamp: Long = 0,
    override var expireUnixTimestamp: Long = 0
) : ITokenData