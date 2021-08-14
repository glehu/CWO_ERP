package modules.mx.logic

import modules.mx.differenceFromUTC
import java.time.LocalDateTime

class MXTimestamp
{
    companion object MXTimestamp
    {
        //Timestamp generation
        fun getTimestamp(): LocalDateTime = LocalDateTime.now()
        fun getUnixTimestampHex() = getUnixTimestamp().toString(16)

        //Timestamp conversion
        fun convUnixHexToUnixTimestamp(unixHex: String) = java.lang.Long.parseLong(unixHex, 16)
        fun getUTCTimestamp(unixLong: Long): String = getUTCTimestampFromUnix(unixLong)
        fun getLocalTimestamp(unixLong: Long): String =
            getUTCTimestampFromUnix(unixLong + (differenceFromUTC * 3600))

        //Internal
        private fun getUnixTimestamp() = (System.currentTimeMillis() / 1000)
        private fun getUTCTimestampFromUnix(unixLong: Long) =
            java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.ofEpochSecond(unixLong))
    }
}