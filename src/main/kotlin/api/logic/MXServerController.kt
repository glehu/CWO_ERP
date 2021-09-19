package api.logic

import interfaces.IIndexManager
import io.ktor.application.*
import io.ktor.auth.*
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
                when (appCall.request.queryParameters["type"]) {
                    "uid" -> {
                        indexManager.setEntryLock(routePar.toInt(), true)
                        return indexManager.getBytes(routePar.toInt())
                    }
                    "name" -> {
                        return indexManager.getEntryBytesListJson(routePar, 1)
                    }
                    else -> return ""
                }
            }
            return ""
        }

        fun getEntryLock(appCall: ApplicationCall, indexManager: IIndexManager): Boolean {
            val routePar = appCall.parameters["searchString"]
            if (routePar != null && routePar.isNotEmpty()) {
                return indexManager.getEntryLock(routePar.toInt())
            }
            return false
        }

        fun setEntryLock(appCall: ApplicationCall, indexManager: IIndexManager): Boolean {
            val routePar = appCall.parameters["searchString"]
            val queryPar = appCall.request.queryParameters["type"]
            if (routePar != null && routePar.isNotEmpty()) {
                return indexManager.setEntryLock(
                    uID = routePar.toInt(),
                    locked = queryPar.toBoolean(),
                    userName = appCall.principal<UserIdPrincipal>()?.name!!
                )
            }
            return false
        }
    }
}