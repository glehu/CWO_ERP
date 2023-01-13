package modules.m5

import com.benasher44.uuid.Uuid
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
  @SerialName("usr") var username: String,
  var roles: ArrayList<String>,
) {
  @SerialName("id")
  var id: String = ""

  @SerialName("fcm")
  var firebaseCloudMessagingToken = ""

  @SerialName("pem")
  var pubKeyPEM = ""

  @SerialName("iurl")
  var imageURL = ""

  @SerialName("burl")
  var bannerURL = ""

  init {
    if (id.isEmpty()) id = Uuid.randomUUID().toString()
  }
}
