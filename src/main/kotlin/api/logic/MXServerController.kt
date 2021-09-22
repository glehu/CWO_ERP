package api.logic

import api.misc.json.LoginResponseJson
import api.misc.json.ValidationContainerJson
import interfaces.IIndexManager
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.mx.MXUser
import modules.mx.logic.encryptKeccak

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
                        return if (appCall.request.queryParameters["format"] == "json") {
                            indexManager.encodeToJsonString(
                                entry = indexManager.decode(
                                    indexManager.getBytes(uID = routePar.toInt())
                                ),
                                prettyPrint = true
                            )
                        } else {
                            indexManager.getBytes(uID = routePar.toInt())
                        }
                    }
                    "name" -> {
                        return if (appCall.request.queryParameters["format"] == "json") {
                            indexManager.getEntryListJson(
                                searchText = routePar,
                                ixNr = 1,
                            )
                        } else {
                            indexManager.getEntryBytesListJson(
                                searchText = routePar,
                                ixNr = 1
                            )
                        }
                    }
                    else -> return ""
                }
            }
            return ""
        }

        fun getEntryLock(appCall: ApplicationCall, indexManager: IIndexManager): Boolean {
            val routePar = appCall.parameters["searchString"]
            if (routePar != null && routePar.isNotEmpty()) {
                return indexManager.getEntryLock(
                    uID = routePar.toInt(),
                    userName = appCall.principal<UserIdPrincipal>()?.name!!
                )
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

        @InternalAPI
        fun generateLoginResponse(user: MXUser): ValidationContainerJson {
            val loginResponse = Json.encodeToString(
                LoginResponseJson(
                    httpCode = 200,
                    accessM1 = user.canAccessM1,
                    accessM2 = user.canAccessM2,
                    accessM3 = user.canAccessM3,
                    accessM4 = user.canAccessM4
                )
            )
            return ValidationContainerJson(
                contentJson = loginResponse,
                hash = encryptKeccak(
                    input = loginResponse,
                    salt = encryptKeccak(user.username),
                    pepper = "CWO_ERP LoginValidation"
                )
            )
        }
    }
}