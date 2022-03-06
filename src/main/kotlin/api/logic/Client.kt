package api.logic

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.activeUser
import modules.mx.logic.Timestamp
import modules.mx.logic.UserCLIManager

/**
 * If the software is being run in client mode it needs to communicate with the server to create and edit data.
 * @return an instance of an authorized HttpClient.
 */
fun getUserClient(username: String = activeUser.username, password: String = activeUser.password): HttpClient {
  return HttpClient(CIO) {
    install(JsonFeature) {
      serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
        isLenient = true
        ignoreUnknownKeys = true
      })
    }
    install(Auth) {
      basic {
        credentials {
          BasicAuthCredentials(username = username, password = password)
        }
        sendWithoutRequest { request ->
          request.url.host == "0.0.0.0"
        }
      }
    }
  }
}

@ExperimentalSerializationApi
@InternalAPI
fun getTokenClient(token: String = activeUser.apiToken.accessToken): HttpClient {
  if (activeUser.apiToken.expireUnixTimestamp <= Timestamp.getUnixTimestamp()) {
    UserCLIManager().login(activeUser.username, activeUser.password)
  }
  return HttpClient(CIO) {
    install(JsonFeature) {
      serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
        isLenient = true
        ignoreUnknownKeys = true
      })
    }
    install(Auth) {
      bearer {
        loadTokens {
          BearerTokens(token, "")
        }
      }
    }
  }
}
