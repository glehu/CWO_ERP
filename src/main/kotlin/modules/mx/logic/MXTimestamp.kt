package modules.mx.logic

import modules.mx.differenceFromUTC

class MXTimestamp {
    companion object MXTimestamp {
        //Timestamp generation
        fun getUnixTimestamp() = (System.currentTimeMillis() / 1000)
        fun getUnixTimestampHex() = getUnixTimestamp().toString(16)

        //Timestamp conversion
        fun convUnixHexToUnixTimestamp(unixHex: String) = java.lang.Long.parseLong(unixHex, 16)
        fun getUTCTimestamp(unixLong: Long): String = getUTCTimestampFromUnix(unixLong)
        fun getLocalTimestamp(unixLong: Long): String =
            getUTCTimestampFromUnix(unixLong + (differenceFromUTC * 3600))

        fun getLocalHour(hour: Int): Int = hour + differenceFromUTC
        fun getLocalHour(hour: String): String = getLocalHour(hour.toInt()).toString()
        fun getUTCTimestampFromUnix(unixLong: Long): String =
            java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.ofEpochSecond(unixLong))

        /**
         * @return the UTC timestamp of the current time.
         */
        fun now(): String {
            return getUTCTimestampFromUnix(getUnixTimestamp())
        }
    }
}
