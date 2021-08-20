package interfaces

import modules.mx.logic.MXTimestamp

interface ITokenData
{
    var accessToken: String
    var tokenType: String
    var scope: String
    var expiresIn: Int
    var refreshToken: String
    var generatedAtUnixTimestamp: Long
    var expireUnixTimestamp: Long

    fun initialize()
    {
        generatedAtUnixTimestamp = MXTimestamp.getUnixTimestamp()
        expireUnixTimestamp = generatedAtUnixTimestamp + expiresIn
    }
}