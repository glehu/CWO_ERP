package api.misc.json

import kotlinx.serialization.Serializable

@Serializable
data class EntryBytesListJson(
    var total: Int,
    val resultsList: ArrayList<ByteArray>
)

@Serializable
data class EntryListJson(
    var total: Int,
    val resultsList: ArrayList<String>
)

@Serializable
data class EntryJson(
    var uID: Int,
    var entry: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EntryJson
        if (!entry.contentEquals(other.entry)) return false
        return true
    }

    override fun hashCode(): Int {
        return entry.contentHashCode()
    }
}

@Serializable
data class LoginResponseJson(
    val httpCode: Int,
    val accessM1: Boolean,
    val accessM2: Boolean,
    val accessM3: Boolean,
    val accessM4: Boolean
)

/**
 * This validation container can be used to send or store data with its hash value.
 * A blockchain can be implemented using this container as the block.
 */
@Serializable
data class ValidationContainerJson(
    val contentJson: String,
    val hash: String
)

@Serializable
data class ListDeltaJson(
    val listEntryNew: String,
    val listEntryOld: String
)

@Serializable
data class WebshopOrder(
    val itemUIDs: Array<Int>,
    val customerNote: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as WebshopOrder
        if (!itemUIDs.contentEquals(other.itemUIDs)) return false
        return true
    }

    override fun hashCode(): Int {
        return itemUIDs.contentHashCode()
    }
}

@Serializable
data class RegistrationPayload(
    val username: String,
    val password: String
)

@Serializable
data class RegistrationResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class LogMsg(
    var id: Int,
    var tstamp: String = "",
    var type: String = "",
    var caller: String = "",
    var msg: String,
    var user: String = "",
    var apiEndpoint: String = ""
)

@Serializable
data class UsageTrackerStats(
    var totalAPICalls: Long
)

@Serializable
data class UsageTrackerData(
    val source: String,
    val module: String,
    val action: String
)

@Serializable
data class EMailJson(
    val subject: String,
    val body: String,
    val recipient: String
)

@Serializable
data class M3Ini(
    var statusTexts: MutableMap<Int, String> = mutableMapOf(),
    var todoStatuses: String = "0",
    var autoCommission: Boolean = true,
    var autoCreateContacts: Boolean = true,
    var autoSendEMailConfirmation: Boolean = true
) {
    init {
        val default = mutableMapOf(
            0 to "Draft",
            1 to "Commissioned",
            2 to "Partially Paid",
            3 to "Paid",
            8 to "Cancelled",
            9 to "Finished"
        )
        for ((k, v) in default) {
            if (!statusTexts.containsKey(k)) {
                statusTexts[k] = v
            }
        }
    }
}

@Serializable
data class MGXEMailerIni(
    var defaultFooter: String = "",
    var writeStatistics: Boolean = true
)

@Serializable
data class SettingsRequestJson(
    val module: String,
    val subSetting: String
)
