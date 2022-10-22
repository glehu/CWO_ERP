package modules.mx.logic

import modules.mx.differenceFromUTC

class Timestamp {
  companion object Timestamp {/*
    Timestamp generation
     */
    /**
     * @return the UTC timestamp of the current time.
     */
    fun now(): String {
      return getUTCTimestampFromUnix(getUnixTimestamp() + (differenceFromUTC * 3600))
    }

    fun getUnixTimestamp() = (System.currentTimeMillis() / 1000)
    fun getUnixTimestampHex() = getUnixTimestamp().toString(16)

    /*
    Timestamp conversion
     */

    fun getUTCTimestamp(unixLong: Long): String = getUTCTimestampFromUnix(unixLong)
    fun getUTCTimestampFromHex(unixHex: String): String = getUTCTimestampFromUnix(convUnixHexToUnixTimestamp(unixHex))

    fun getLocalTimestamp(unixLong: Long): String = getUTCTimestampFromUnix(unixLong + (differenceFromUTC * 3600))

    /*
    Helper functions
     */
    fun convUnixHexToUnixTimestamp(unixHex: String) = java.lang.Long.parseLong(unixHex, 16)
    fun getUTCTimestampFromUnix(unixLong: Long): String =
      java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.ofEpochSecond(unixLong))
  }
}
