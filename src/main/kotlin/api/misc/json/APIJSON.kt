package api.misc.json

import interfaces.ITokenData
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import modules.m5.UniChatroom
import modules.m5.UniRole
import modules.m7wisdom.Wisdom

@Serializable
data class EntryBytesListJson(
  var total: Int, val resultsList: ArrayList<ByteArray>
)

@Serializable
data class EntryListJson(
  var total: Int, val resultsList: ArrayList<String>
)

@Serializable
data class EntryJson(
  var uID: Int, var entry: ByteArray
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
  val username: String,
  val expiresInMs: Int,
  val accessM1: Boolean,
  val accessM2: Boolean,
  val accessM3: Boolean,
  val accessM4: Boolean,
  val accessM5: Boolean,
  val accessM6: Boolean
)

@Serializable
data class CWOAuthCallbackJson(
  @SerialName("access_token") override var accessToken: String = "?",
  @SerialName("token_type") override var tokenType: String = "?",
  override var scope: String = "?",
  @SerialName("expires_in") override var expiresInSeconds: Int = 0,
  @SerialName("refresh_token") override var refreshToken: String = "", //Automatic
  override var generatedAtUnixTimestamp: Long = 0,
  override var expireUnixTimestamp: Long = 0
) : ITokenData

/**
 * This validation container can be used to send or store data with its hash value.
 * A blockchain can be implemented using this container as the block.
 */
@Serializable
data class ValidationContainerJson(
  val contentJson: String, val hash: String
)

@Serializable
data class ListDeltaJson(
  val listEntryNew: String, val listEntryOld: String
)

@Serializable
data class WebshopOrder(
  val cart: Array<WebshopCartItem>, val customerNote: String = ""
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
  val uID: Long,
  val description: String,
  val amount: Int,
  val price: Double,
  @SerialName("imageBase64String") val imgBase64: String = ""
)

@Serializable
data class RegistrationPayload(
  val email: String, val username: String, val password: String
)

@Serializable
data class RegistrationResponse(
  val success: Boolean, val message: String
)

@Serializable
data class UsageTrackerStats(
  var totalAPICalls: Long
)

@Serializable
data class UsageTrackerData(
  val source: String, val module: String, val action: String
)

@Serializable
data class EMailJson(
  val subject: String, val body: String, val recipient: String
)

@Serializable
data class SettingsRequestJson(
  val module: String, val subSetting: String
)

@Serializable
data class TwoLongOneDoubleJson(
  val first: Long, val second: Long, val third: Double
)

@Serializable
data class PairLongJson(
  val first: Long, val second: Long
)

@Serializable
data class WebPlannerCommit(
  val action: String, val project: String, val cells: Array<WebPlannerCell>
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as WebPlannerCommit
    if (!cells.contentEquals(other.cells)) return false
    return true
  }

  override fun hashCode(): Int {
    return cells.contentHashCode()
  }
}

@Serializable
data class WebPlannerRequest(
  val action: String, val project: String
)

@Serializable
data class WebPlannerResponse(
  val type: String, val content: Array<WebPlannerCell>
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as WebPlannerResponse
    if (!content.contentEquals(other.content)) return false
    return true
  }

  override fun hashCode(): Int {
    return content.contentHashCode()
  }
}

@Serializable
data class WebPlannerCell(
  val x: Int,
  val y: Int,
  val id: String,
  val type: String,
  val rows: Int,
  val box: String,
  val history: Array<String>,
  val name: String = "",
  val description: String = "",
  val comments: Array<String>
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as WebPlannerCell
    if (!history.contentEquals(other.history)) return false
    if (!comments.contentEquals(other.comments)) return false
    return true
  }

  override fun hashCode(): Int {
    var result = history.contentHashCode()
    result = 31 * result + comments.contentHashCode()
    return result
  }
}

@Serializable
data class WebMockingbirdConfig(
  val config: MockingbirdConfig
)

@Serializable
data class MockingbirdConfig(
  val content_type: String,
  val message_type: String,
  val return_type: String,
  val return_message: String,
  val return_code: String,
  val return_redirect: String,
  val return_delay: String,
  val return_delay_unit: String
)

@Serializable
data class UniChatroomCreateChatroom(
  val title: String,
  /** Determines the type of this chatroom.
   *
   * Possible values:
   * - text
   * - screenshare
   * - webcam
   * - direct
   */
  val type: String, val imgBase64: String = "", val directMessageUsernames: Array<String> = arrayOf()
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as UniChatroomCreateChatroom
    if (!directMessageUsernames.contentEquals(other.directMessageUsernames)) return false
    return true
  }

  override fun hashCode(): Int {
    return directMessageUsernames.contentHashCode()
  }
}

@Serializable
data class UniChatroomAddMessage(
  val uniChatroomGUID: String, val text: String
)

@Serializable
data class UniChatroomEditMessage(
  val uniMessageGUID: String, val newContent: String
)

@Serializable
data class UniChatroomReactMessage(
  val uniMessageGUID: String, val type: String
)

@Serializable
data class UniChatroomReactMessageResponse(
  val uniMessageGUID: String, val type: String, val from: String, val isRemove: Boolean
)

@Serializable
data class UniMessageReaction(
  @SerialName("src") val from: ArrayList<String>, @SerialName("t") val type: String
)

@Serializable
data class UniChatroomAddMember(
  val uniChatroomGUID: String, val member: String, val role: String
)

@Serializable
data class UniChatroomRemoveMember(
  val member: String
)

@Serializable
data class UniChatroomMemberRole(
  val member: String, val role: String
)

@Serializable
data class FirebaseCloudMessagingSubscription(
  val fcmToken: String
)

@Serializable
data class PubKeyPEMContainer(
  val pubKeyPEM: String
)

@Serializable
data class UniChatroomImage(
  val imageBase64: String
)

@Serializable
data class UniMemberProfileImage(
  val imageBase64: String, val username: String
)

@Serializable
data class UniChatroomMessages(
  var messages: ArrayList<String>
)

@Serializable
data class SnippetPayload(
  val type: String, val payload: String = ""
)

@Serializable
data class SnippetResponse(
  val httpCode: Int, val guid: String
)

@Serializable
data class UsernameChange(
  val username: String, val newUsername: String
)

@Serializable
data class PasswordChange(
  val username: String, val password: String, val newPassword: String
)

@Serializable
data class LeaderboardStatsAdvanced(
  var username: String,
  var messages: Int = 0,
  var reactions: Int = 0,
  var totalRating: Double = 0.0,
  var amountMSG: Int = 0,
  var amountIMG: Int = 0,
  var amountGIF: Int = 0,
  var amountAUD: Int = 0
)

@Serializable
data class UniChatroomUpgrade(
  val toRank: Int
)

@Serializable
data class KnowledgeCreation(
  val mainChatroomGUID: String = "",
  val title: String = "",
  val description: String = "",
  val keywords: String = "",
  val isPrivate: Boolean = true
)

@Serializable
data class KnowledgeCategoryEdit(
  val action: String = "", val category: String = ""
)

@Serializable
data class WisdomQuestionCreation(
  val title: String = "",
  val description: String = "",
  val knowledgeGUID: String = "",
  var keywords: String = "",
  val categories: ArrayList<String> = arrayListOf(),
  var copyContent: String = ""
)

@Serializable
data class WisdomAnswerCreation(
  val title: String = "",
  val description: String = "",
  val wisdomGUID: String = "",
  val keywords: String = "",
  var copyContent: String = ""
)

@Serializable
data class WisdomLessonCreation(
  val title: String = "",
  val description: String = "",
  val knowledgeGUID: String = "",
  val keywords: String = "",
  var copyContent: String = "",
  val categories: ArrayList<String> = arrayListOf(),
  val isTask: Boolean = false,
  val taskType: String = "",
  val columnIndex: Int = 0,
  val rowIndex: Int = 0,
  val inBox: Boolean = false,
  val boxGUID: String = "",
  val hasDueDate: Boolean = false,
  val dueDate: String = "",
  val dueDateUntil: String = ""
)

@Serializable
data class WisdomCommentCreation(
  val title: String = "",
  val description: String = "",
  val wisdomGUID: String = "",
  val keywords: String = "",
)

@Serializable
data class WisdomSearchQuery(
  val query: String = "",
  /**
   * wisdom or task
   */
  val type: String = "wisdom",
  val categories: ArrayList<String> = arrayListOf(),
  val filterOverride: String = "",
  val entryType: String = "",
  val state: String = ""
)

@ExperimentalSerializationApi
@InternalAPI
@Serializable
data class WisdomSearchResponse(
  var first: List<WisdomSearchResponseEntry> = arrayListOf(),
  var second: List<WisdomSearchResponseEntry> = arrayListOf(),
  var third: List<WisdomSearchResponseEntry> = arrayListOf(),
  var time: Int = -1
)

@ExperimentalSerializationApi
@InternalAPI
@Serializable
data class WisdomSearchResponseEntry(
  val wisdom: Wisdom, val accuracy: Int
)

@ExperimentalSerializationApi
@InternalAPI
@Serializable
data class WisdomReferencesResponse(
  val answers: ArrayList<Wisdom> = arrayListOf(),
  val comments: ArrayList<Wisdom> = arrayListOf(),
  val tasks: ArrayList<Wisdom> = arrayListOf(),
  val questions: ArrayList<Wisdom> = arrayListOf(),
  val lessons: ArrayList<Wisdom> = arrayListOf()
)

@ExperimentalSerializationApi
@InternalAPI
@Serializable
data class WisdomTopContributorsResponse(
  var contributors: ArrayList<WisdomTopContributorsResponseEntry> = arrayListOf(),
)

@ExperimentalSerializationApi
@InternalAPI
@Serializable
data class WisdomTopContributorsResponseEntry(
  val username: String,
  val imageURL: String,
  val lessons: Int,
)

@ExperimentalSerializationApi
@InternalAPI
@Serializable
data class TaskBoxesResponse(
  var boxes: ArrayList<TaskBoxPayload> = arrayListOf()
)

@ExperimentalSerializationApi
@InternalAPI
@Serializable
data class TaskBoxPayload(
  var box: Wisdom, var tasks: ArrayList<Wisdom> = arrayListOf()
)

@Serializable
data class WisdomHistoryEntry(
  var type: String = "",
  var date: String = "",
  @SerialName("desc") var description: String = "",
  @SerialName("author") var authorUsername: String = ""
)

data class WisdomCollaboratorPayload(
  var username: String, var roles: Array<UniRole> = arrayOf(), var add: Boolean = true
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as WisdomCollaboratorPayload
    if (!roles.contentEquals(other.roles)) return false
    return true
  }

  override fun hashCode(): Int {
    return roles.contentHashCode()
  }
}

@ExperimentalSerializationApi
@InternalAPI
@Serializable
data class ActiveMembersPayload(
  var members: ArrayList<String> = arrayListOf()
)

@ExperimentalSerializationApi
@InternalAPI
@Serializable
data class KeywordsPayload(
  var keywords: ArrayList<String> = arrayListOf()
)

@ExperimentalSerializationApi
@InternalAPI
@Serializable
data class CategoriesPayload(
  var categories: ArrayList<CategoryPayload> = arrayListOf()
)

@ExperimentalSerializationApi
@InternalAPI
@Serializable
data class CategoryPayload(
  var category: String, var count: Int
)

@ExperimentalSerializationApi
@InternalAPI
@Serializable
data class ChatroomsPayload(
  var chatrooms: ArrayList<UniChatroom> = arrayListOf()
)

data class FriendRequestResponse(
  var successful: Boolean,
  var message: String
)
