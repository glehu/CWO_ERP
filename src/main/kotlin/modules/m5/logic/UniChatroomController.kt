package modules.m5.logic

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m5.UniChatroom
import modules.mx.logic.Log
import modules.mx.logic.Timestamp
import modules.mx.uniChatroomIndexManager

@ExperimentalSerializationApi
@InternalAPI
class UniChatroomController : IModule {
  override val moduleNameLong = "UniChatroomController"
  override val module = "M5"
  override fun getIndexManager(): IIndexManager {
    return uniChatroomIndexManager!!
  }

  fun createChatroom(title: String): UniChatroom {
    log(Log.LogType.COM, "Chatroom created")
    return UniChatroom(-1, title)
  }

  fun getChatroom(chatroomGUID: String): UniChatroom? {
    var uniChatroom: UniChatroom?
    synchronized(this) {
      uniChatroom = null
      getEntriesFromIndexSearch(chatroomGUID, 2, true) {
        uniChatroom = it as UniChatroom
      }
    }
    return uniChatroom
  }

  suspend fun saveChatroom(uniChatroom: UniChatroom) {
    save(uniChatroom)
  }

  fun UniChatroom.addMember(member: String, role: String): Boolean {
    this.members[member] = role
    log(Log.LogType.COM, "Member added")
    return true
  }

  fun UniChatroom.addMessage(member: String, message: String): Boolean {
    if (!checkMessage(message)) {
      log(Log.LogType.ERROR, "Message invalid (checkMessage)")
      return false
    }
    if (!this.checkMember(member)) {
      log(Log.LogType.ERROR, "Message invalid (checkMember)")
      return false
    }
    this.messages.add(Json.encodeToString(UniMessage(member, Timestamp.getUnixTimestampHex(), message)))
    log(Log.LogType.COM, "Message added")
    return true
  }

  private fun checkMessage(message: String): Boolean {
    return message.isNotEmpty()
  }

  private fun UniChatroom.checkMember(member: String): Boolean {
    if (!this.members.containsKey(member)) return false
    if (this.banlist.containsKey(member)) return false
    return true
  }
}
