package modules.m5

import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi

@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
@kotlinx.serialization.Serializable
data class UniMember(
  var username: String,
  var roles: ArrayList<String>,
) {
  var firebaseCloudMessagingToken = ""
}
