package modules.mx

import kotlinx.serialization.Serializable

@Serializable
data class MXUser(var username: String, var password: String)