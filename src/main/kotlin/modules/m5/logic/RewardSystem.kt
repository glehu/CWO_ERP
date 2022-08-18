package modules.m5.logic

import api.logic.core.ServerController
import api.misc.json.LeaderboardStatsAdvanced
import api.misc.json.UniChatroomUpgrade
import api.misc.json.UniMessageReaction
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m2.Contact
import modules.m5.UniBadge
import modules.m5.UniChatroom
import modules.m5.UniMember
import modules.m5.UniRole
import modules.m5messages.UniMessage
import modules.mx.contactIndexManager
import modules.mx.logic.Log
import modules.mx.logic.Timestamp
import modules.mx.logic.UserCLIManager
import modules.mx.uniMessagesIndexManager

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
suspend fun giveMessagesBadges(chatroomGUID: String?, appCall: ApplicationCall) {
  if (chatroomGUID.isNullOrEmpty()) {
    appCall.respond(HttpStatusCode.BadRequest)
    return
  }
  val chatroom = UniChatroomController().getMainChatroom(chatroomGUID)
  if (chatroom == null) {
    appCall.respond(HttpStatusCode.NotFound)
    return
  }
  // Check eligibility status...
  if (!isChatroomEligibleForRewards(chatroom)) {
    appCall.respond(HttpStatusCode.Forbidden)
    return
  }
  // Get advanced member stats
  val memberStats: MutableMap<String, LeaderboardStatsAdvanced> = getMemberStatsOfChatroom(chatroom)
  if (memberStats.isEmpty()) {
    appCall.respond(HttpStatusCode.ExpectationFailed)
    return
  }
  for (statistic in memberStats) {
    var user = UserCLIManager.getUserFromUsername(statistic.key) ?: continue
    // Distribute badges for message count
    user = runMessageBadgeDist(user, statistic)
    // Distribute badges for rating
    user = runRatingBadgeDist(user, statistic)
    // Save badges
    contactIndexManager!!.save(user)
  }
  appCall.respond(HttpStatusCode.OK)
}

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
suspend fun getBadges(username: String?, appCall: ApplicationCall) {
  if (username.isNullOrEmpty()) {
    appCall.respond(HttpStatusCode.BadRequest)
    return
  }
  val user = UserCLIManager.getUserFromUsername(username)
  if (user == null) {
    appCall.respond(HttpStatusCode.NotFound)
    return
  }
  appCall.respond(user.badges)
}

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
fun runMessageBadgeDist(
  user: Contact, statistic: MutableMap.MutableEntry<String, LeaderboardStatsAdvanced>
): Contact {
  var userTmp = user
  userTmp = statistic.value.checkAndPutMessageBadge(
    user = userTmp, threshold = 100, badge = UniBadge(
      title = "Talkative I.",
      handle = "msg100",
      description = "Earned for having sent a total of 100 messages.",
      xpGain = 50,
      rank = 1,
      Timestamp.now()
    )
  )
  userTmp = statistic.value.checkAndPutMessageBadge(
    user = userTmp, threshold = 500, badge = UniBadge(
      title = "Talkative II.",
      handle = "msg500",
      description = "Earned for having sent a total of 500 messages.",
      xpGain = 150,
      rank = 2,
      Timestamp.now()
    )
  )
  userTmp = statistic.value.checkAndPutMessageBadge(
    user = userTmp, threshold = 1000, badge = UniBadge(
      title = "Talkative III.",
      handle = "msg1000",
      description = "Earned for having sent a total of 1,000 messages.",
      xpGain = 450,
      rank = 3,
      Timestamp.now()
    )
  )
  userTmp = statistic.value.checkAndPutMessageBadge(
    user = userTmp, threshold = 2000, badge = UniBadge(
      title = "Talkative IV.",
      handle = "msg2000",
      description = "Earned for having sent a total of 2,000 messages.",
      xpGain = 1350,
      rank = 4,
      Timestamp.now()
    )
  )
  userTmp = statistic.value.checkAndPutMessageBadge(
    user = userTmp, threshold = 5000, badge = UniBadge(
      title = "Talkative V.",
      handle = "msg4000",
      description = "Earned for having sent a total of 5,000 messages.",
      xpGain = 4050,
      rank = 5,
      Timestamp.now()
    )
  )
  return userTmp
}

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
fun runRatingBadgeDist(
  user: Contact, statistic: MutableMap.MutableEntry<String, LeaderboardStatsAdvanced>
): Contact {
  var userTmp = user
  userTmp = statistic.value.checkAndPutRatingBadge(
    user = userTmp, threshold = 10, badge = UniBadge(
      title = "Guru I.",
      handle = "rt10",
      description = "Earned for having a rating of at least 10.",
      xpGain = 50,
      rank = 1,
      Timestamp.now()
    )
  )
  userTmp = statistic.value.checkAndPutRatingBadge(
    user = userTmp, threshold = 100, badge = UniBadge(
      title = "Guru II.",
      handle = "rt50",
      description = "Earned for having a rating of at least 100.",
      xpGain = 150,
      rank = 2,
      Timestamp.now()
    )
  )
  userTmp = statistic.value.checkAndPutRatingBadge(
    user = userTmp, threshold = 200, badge = UniBadge(
      title = "Guru III.",
      handle = "rt100",
      description = "Earned for having a rating of at least 200.",
      xpGain = 450,
      rank = 3,
      Timestamp.now()
    )
  )
  userTmp = statistic.value.checkAndPutRatingBadge(
    user = userTmp, threshold = 500, badge = UniBadge(
      title = "Guru IV.",
      handle = "rt200",
      description = "Earned for having a rating of at least 500.",
      xpGain = 1350,
      rank = 4,
      Timestamp.now()
    )
  )
  userTmp = statistic.value.checkAndPutRatingBadge(
    user = userTmp, threshold = 1000, badge = UniBadge(
      title = "Guru V.",
      handle = "rt500",
      description = "Earned for having a rating of at least 1000.",
      xpGain = 4050,
      rank = 5,
      Timestamp.now()
    )
  )
  return userTmp
}

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
fun LeaderboardStatsAdvanced.checkAndPutMessageBadge(
  user: Contact, threshold: Int, badge: UniBadge
): Contact {
  if (this.messages > threshold) {
    if (!checkMemberBadge(user, badge.handle)) {
      user.badges.add(Json.encodeToString(badge))
    }
  }
  return user
}

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
fun LeaderboardStatsAdvanced.checkAndPutRatingBadge(
  user: Contact, threshold: Int, badge: UniBadge
): Contact {
  if (this.totalRating > threshold) {
    if (!checkMemberBadge(user, badge.handle)) {
      user.badges.add(Json.encodeToString(badge))
    }
  }
  return user
}

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
fun checkMemberBadge(username: String, badgeHandle: String): Boolean {
  val user = UserCLIManager.getUserFromUsername(username) ?: return false
  return checkMemberBadge(user, badgeHandle)
}

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
fun checkMemberBadge(user: Contact, badgeHandle: String): Boolean {
  if (user.badges.isEmpty()) {
    return false
  }
  for (badge in user.badges) {
    if (Json.decodeFromString<UniBadge>(badge).handle == badgeHandle) return true
  }
  return false
}

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
suspend fun getMemberStatsOfChatroom(
  chatroom: UniChatroom
): MutableMap<String, LeaderboardStatsAdvanced> {
  // Check for subchats... we want to filter all messages, not just the main chatroom's
  val subchatUIDs = StringBuilder()
  // Append main chatroom's uID
  subchatUIDs.append(chatroom.uID)
  if (chatroom.subChatrooms.size > 0) {
    // Append each sub chatroom's uID
    var subchat: UniChatroom?
    for (subchatJson in chatroom.subChatrooms) {
      subchat = UniChatroomController().getChatroom(
        Json.decodeFromString<UniChatroom>(subchatJson).chatroomGUID
      )
      if (subchat != null) {
        subchatUIDs.append("|")
        subchatUIDs.append(subchat.uID)
      }
    }
  }
  val regexString = "($subchatUIDs)"
  Log.log(
    module = "M5", type = Log.Type.INFO, text = "ANALYSIS for Chatroom uIDs: $regexString", caller = "RewardSystem"
  )
  // Map to be populated by Usernames with their message stats
  var memberStats: MutableMap<String, LeaderboardStatsAdvanced> = mutableMapOf()
  uniMessagesIndexManager!!.getEntriesFromIndexSearch(
    searchText = "^$regexString$", ixNr = 1, showAll = true
  ) {
    it as UniMessage
    if (it.from != "_server") {
      if (!memberStats.containsKey(it.from)) {
        memberStats[it.from] = LeaderboardStatsAdvanced(it.from)
      }
      memberStats[it.from]!!.messages += 1
      // Count reactions!
      memberStats = countReactions(it, memberStats)
      // Count each message type
      memberStats = countMessageTypes(it, memberStats)
    }
  }
  return memberStats
}

@ExperimentalSerializationApi
@InternalAPI
fun countMessageTypes(
  message: UniMessage, memberStats: MutableMap<String, LeaderboardStatsAdvanced>
): MutableMap<String, LeaderboardStatsAdvanced> {
  if (message.message.startsWith("[c:GIF]")) {
    memberStats[message.from]!!.amountGIF += 1
  } else if (message.message.startsWith("[c:IMG]")) {
    memberStats[message.from]!!.amountIMG += 1
  } else if (message.message.startsWith("[c:AUD]")) {
    memberStats[message.from]!!.amountAUD += 1
  } else {
    memberStats[message.from]!!.amountMSG += 1
  }
  return memberStats
}

@ExperimentalSerializationApi
@InternalAPI
fun countReactions(
  message: UniMessage, memberStats: MutableMap<String, LeaderboardStatsAdvanced>
): MutableMap<String, LeaderboardStatsAdvanced> {
  if (message.reactions.size > 0) {
    var upvotes = 0.0
    var downvotes = 0.0
    var stars = 0.0
    memberStats[message.from]!!.reactions += message.reactions.size
    for (reaction in message.reactions) {
      val react: UniMessageReaction = Json.decodeFromString(reaction)
      when (react.type) {
        "+" -> {
          upvotes += react.from.size
          if (react.from.contains(message.from)) upvotes -= 1
        }
        "⭐" -> {
          stars += react.from.size
          if (react.from.contains(message.from)) stars -= 1
        }
        "-" -> {
          downvotes += react.from.size
          if (react.from.contains(message.from)) downvotes -= 1
        }
      }
    }
    var rating = 0.0
    // Add likes...
    if (upvotes > 0) rating += (1.0 * upvotes)
    // Multiply by stars...
    if (stars > 0) {
      if (rating == 0.0) {
        rating = 1.0
      }
      rating *= (2.25 * stars)
    }
    // Then subtract downvotes
    if (downvotes > 0) rating -= (1.0 * downvotes)
    // Add the rating
    memberStats[message.from]!!.totalRating += rating
  }
  return memberStats
}

@ExperimentalCoroutinesApi
@ExperimentalSerializationApi
@DelicateCoroutinesApi
@InternalAPI
suspend
        /**
         * Checks if a chatroom (or its parent main chatroom if a subchat GUID was provided) is
         * eligible for the Reward System.
         *
         * Only chatrooms at rank 2 or above can be part of the Reward System.
         *
         * @return true if the provided chatroom or its parent are at rank 2 or above.
         */
fun isChatroomEligibleForRewards(chatroomGUID: String): Boolean {
  if (chatroomGUID.isEmpty()) return false
  val chatroom = UniChatroomController().getChatroom(chatroomGUID) ?: return false
  return isChatroomEligibleForRewards(chatroom)
}

@ExperimentalCoroutinesApi
@ExperimentalSerializationApi
@DelicateCoroutinesApi
@InternalAPI
suspend
        /**
         * Checks if a chatroom (or its parent main chatroom if a subchat GUID was provided) is
         * eligible for the Reward System.
         *
         * Only chatrooms at rank 2 or above can be part of the Reward System.
         *
         * @return true if the provided chatroom or its parent are at rank 2 or above.
         */
fun isChatroomEligibleForRewards(chatroom: UniChatroom): Boolean {
  val mainChatroom = if (chatroom.parentGUID.isNotEmpty()) {
    UniChatroomController().getChatroom(chatroom.parentGUID) ?: return false
  } else chatroom
  return mainChatroom.rank >= 2
}

@ExperimentalCoroutinesApi
@ExperimentalSerializationApi
@DelicateCoroutinesApi
@InternalAPI
suspend fun handleUpgradeUniChatroomRequest(appCall: ApplicationCall, config: UniChatroomUpgrade) {
  val username = UserCLIManager.getUserFromEmail(
    ServerController.getJWTEmail(appCall)
  )!!.username
  val uniChatroomGUID = appCall.parameters["uniChatroomGUID"]
  if (uniChatroomGUID.isNullOrEmpty()) {
    appCall.respond(HttpStatusCode.BadRequest)
    return
  }
  val chatroom = UniChatroomController().getChatroom(uniChatroomGUID)
  if (chatroom == null) {
    appCall.respond(HttpStatusCode.NotFound)
    return
  }
  val mainChatroom = if (chatroom.parentGUID.isNotEmpty()) {
    UniChatroomController().getChatroom(chatroom.parentGUID)
  } else chatroom
  if (mainChatroom == null) {
    appCall.respond(HttpStatusCode.NotFound)
    return
  }
  var isOwner = false
  var member: UniMember
  for (memberJson in mainChatroom.members) {
    member = Json.decodeFromString(memberJson)
    if (member.username == username) {
      // We got the user, now check for the role "Owner"
      for (roleJson in member.roles) {
        if (Json.decodeFromString<UniRole>(roleJson).name.uppercase() == "OWNER") {
          isOwner = true
        }
      }
    }
  }
  if (!isOwner) {
    appCall.respond(HttpStatusCode.Forbidden)
    return
  }
  if (mainChatroom.rank >= config.toRank) {
    appCall.respond(HttpStatusCode.BadRequest)
    return
  }
  // Upgrade chatroom rank
  mainChatroom.rank = config.toRank
  mainChatroom.rankDescription = when (mainChatroom.rank) {
    1 -> "Starter"
    2 -> "Juniors"
    3 -> "Seniors"
    4 -> "Experts"
    5 -> "Masters"
    else -> "!InvalidRank!"
  }
  UniChatroomController().saveChatroom(mainChatroom)
  appCall.respond(HttpStatusCode.OK)
}
