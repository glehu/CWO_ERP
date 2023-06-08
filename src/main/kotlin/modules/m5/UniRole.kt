package modules.m5

import kotlinx.serialization.Serializable

interface IUniRole {
  val name: String
  val canWrite: Boolean
  val canRead: Boolean
}

@Serializable
data class UniRole(
  override val name: String
) : IUniRole {
  override val canRead: Boolean
    get() = true
  override val canWrite: Boolean
    get() = true
}

@Serializable
data class UniChatroomRole(
  override var name: String,
  var priority: Int,
  var canPingEveryone: Boolean,
  var canPingRole: Boolean,
  var canKick: Boolean,
  var canBan: Boolean,
  var canEditSubchat: Boolean,
  var canEditRoles: Boolean,
  var canEditEvents: Boolean,
  var color: String = "",
  override var canRead: Boolean = true,
  override var canWrite: Boolean = true
) : IUniRole
