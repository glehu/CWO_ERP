package api.logic

import interfaces.IIndexManager
import io.ktor.application.*
import kotlinx.serialization.ExperimentalSerializationApi

class MXServerController {
    @ExperimentalSerializationApi
    companion object Server {
        fun saveEntry(entry: ByteArray, indexManager: IIndexManager, username: String): Int {
            return indexManager.save(
                entry = indexManager.decode(entry),
                userName = username
            )
        }

        fun getEntry(appCall: ApplicationCall, indexManager: IIndexManager): Any {
            val routePar = appCall.parameters["searchString"]
            if (routePar != null && routePar.isNotEmpty()) {
                return when (appCall.request.queryParameters["type"]) {
                    "uid" -> {
                        indexManager.getBytes(routePar.toInt())
                    }
                    "name" -> {
                        indexManager.getEntryBytesListJson(routePar, 1)
                    }
                    else -> ""
                }
            }
            return ""
        }
    }
}