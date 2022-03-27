package modules.m5

interface IUniRole {
  val name: String
  val canWrite: Boolean
  val canRead: Boolean
}

@kotlinx.serialization.Serializable
data class UniRole(override val name: String) : IUniRole {
  override val canRead: Boolean
    get() = true
  override val canWrite: Boolean
    get() = true
}
