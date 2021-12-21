package api.misc.json

import interfaces.ITokenData
import kotlinx.serialization.SerialName
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
    val token: String,
    val expiresInMs: Int,
    val accessM1: Boolean,
    val accessM2: Boolean,
    val accessM3: Boolean,
    val accessM4: Boolean
)

@Serializable
data class CWOAuthCallbackJson(
    @SerialName("access_token")
    override var accessToken: String = "?",
    @SerialName("token_type")
    override var tokenType: String = "?",
    override var scope: String = "?",
    @SerialName("expires_in")
    override var expiresInSeconds: Int = 0,
    @SerialName("refresh_token")
    override var refreshToken: String = "",
    //Automatic
    override var generatedAtUnixTimestamp: Long = 0,
    override var expireUnixTimestamp: Long = 0
) : ITokenData

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
    val cart: Array<WebshopCartItem>,
    val customerNote: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as WebshopOrder
        if (!cart.contentEquals(other.cart)) return false
        if (customerNote != other.customerNote) return false
        return true
    }

    override fun hashCode(): Int {
        var result = cart.contentHashCode()
        result = 31 * result + customerNote.hashCode()
        return result
    }
}

@Serializable
data class WebshopCartItem(
    val uID: Int,
    val description: String,
    val amount: Int,
    val price: Double,
    @SerialName("imageBase64String")
    val imgBase64: String = ""
)

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
data class SettingsRequestJson(
    val module: String,
    val subSetting: String
)
