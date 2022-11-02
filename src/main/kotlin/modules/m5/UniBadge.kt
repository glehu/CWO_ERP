package modules.m5

@kotlinx.serialization.Serializable
data class UniBadge(
  val title: String, val handle: String, val description: String, val xpGain: Int, val rank: Int, val dateEarned: String
)
