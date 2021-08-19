package modules.interfaces

import modules.mx.logic.MXTimestamp

interface ITokenData
{
    var access_token: String
    var token_type: String
    var scope: String
    var expires_in: Int
    var refresh_token: String
    var generatedAtUnixTimestamp: Long
    var expireUnixTimestamp: Long

    fun initialize()
    {
        generatedAtUnixTimestamp = MXTimestamp.getUnixTimestamp()
        expireUnixTimestamp = generatedAtUnixTimestamp + expires_in
    }
}