package modules.m7wisdom

import modules.m5.UniRole

@kotlinx.serialization.Serializable
data class WisdomCollaborator(
  var username: String,
  var roles: Array<UniRole> = arrayOf()
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as WisdomCollaborator
    if (!roles.contentEquals(other.roles)) return false
    return true
  }

  override fun hashCode(): Int {
    return roles.contentHashCode()
  }
}
