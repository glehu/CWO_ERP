package modules.m5

@kotlinx.serialization.Serializable
data class UniMember(
  val username: String,
  val roles: ArrayList<String>
)
