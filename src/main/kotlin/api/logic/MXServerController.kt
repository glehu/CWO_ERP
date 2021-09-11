package api.logic

import interfaces.IIndexManager
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.request.*
import io.ktor.response.*
import kotlinx.serialization.ExperimentalSerializationApi

class MXServerController {
    @ExperimentalSerializationApi
    companion object Server {
        suspend fun saveEntry(appCall: ApplicationCall, indexManager: IIndexManager): Int {
            return indexManager.save(
                entry = indexManager.decode(appCall.receive()),
                userName = appCall.principal<UserIdPrincipal>()!!.name
            )
        }

        suspend fun getEntry(appCall: ApplicationCall, indexManager: IIndexManager) {
            val routePar = appCall.parameters["searchString"]
            if (routePar != null && routePar.isNotEmpty()) {
                val queryPar = appCall.request.queryParameters["type"]
                if (queryPar == "uid") {
                    appCall.respond(indexManager.getBytes(routePar.toInt()))
                } else if (queryPar == "name") {
                    appCall.respond(indexManager.getEntryBytesListJson(routePar, 1))
                }
            }
        }
    }
}