package modules.interfaces

import modules.mx.logic.MXTimestamp

interface ITokenData
{
    val access_token: String
    val token_type: String
    val scope: String
    val expires_in: Int
    val refresh_token: String
    var generatedAtUnixTimestamp: Long
    var expireUnixTimestamp: Long

    fun initialize()
    {
        generatedAtUnixTimestamp = MXTimestamp.getUnixTimestamp()
        expireUnixTimestamp = generatedAtUnixTimestamp + expires_in
    }
}