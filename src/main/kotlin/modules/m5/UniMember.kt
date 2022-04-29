package modules.m5

import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName

@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
@kotlinx.serialization.Serializable
data class UniMember(
  @SerialName("usr")
  var username: String,
  var roles: ArrayList<String>,
) {
  @SerialName("fcm")
  var firebaseCloudMessagingToken = ""
}
