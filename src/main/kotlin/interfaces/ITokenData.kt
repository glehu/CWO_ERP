package interfaces

import modules.mx.logic.Timestamp

interface ITokenData {
  var accessToken: String
  var tokenType: String
  var scope: String
  var expiresInSeconds: Int
  var refreshToken: String
  var generatedAtUnixTimestamp: Long
  var expireUnixTimestamp: Long

  fun initialize() {
    generatedAtUnixTimestamp = Timestamp.getUnixTimestamp()
    expireUnixTimestamp = generatedAtUnixTimestamp + expiresInSeconds
  }
}
