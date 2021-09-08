package api.logic

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*

/**
 * If the software is being run in client mode it needs to communicate with the server to create and edit data.
 * @return an instance of an authorized HttpClient.
 */
fun getCWOClient(username: String, password: String): HttpClient {
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