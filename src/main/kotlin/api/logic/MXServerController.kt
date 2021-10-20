package api.logic

import api.misc.json.LoginResponseJson
import api.misc.json.UPPriceCategoryJson
import api.misc.json.ValidationContainerJson
import interfaces.IIndexManager
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m4Items.logic.M4PriceManager
import modules.mx.MXUser
import modules.mx.logic.encryptKeccak

class MXServerController {
    @ExperimentalSerializationApi
    companion object Server {
        suspend fun saveEntry(entry: ByteArray, indexManager: IIndexManager, username: String): Int {
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
                        val doLock = when (appCall.request.queryParameters["lock"]) {
                            "true" -> true
                            else -> false
                        }
                        return if (appCall.request.queryParameters["format"] == "json") {
                            indexManager.encodeToJsonString(
                                entry = indexManager.decode(
                                    indexManager.getBytes(uID = routePar.toInt())
                                ),
                                prettyPrint = true
                            )
                        } else {
                            indexManager.getBytes(
                                uID = routePar.toInt(),
                                lock = doLock,
                                userName = appCall.principal<UserIdPrincipal>()?.name!!
                            )
                        }
                    }
                    "name" -> {
                        val requestIndex = appCall.request.queryParameters["index"]
                        val ixNr: Int = (requestIndex?.toInt()) ?: 1
                        val exactSearch = requestIndex != null
                        return if (appCall.request.queryParameters["format"] == "json") {
                            indexManager.getEntryListJson(
                                searchText = routePar,
                                ixNr,
                                exactSearch = exactSearch
                            )
                        } else {
                            indexManager.getEntryBytesListJson(
                                searchText = routePar,
                                ixNr,
                                exactSearch = exactSearch
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
                    doLock = queryPar.toBoolean(),
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
                    pepper = encryptKeccak("CWO_ERP LoginValidation")
                )
            )
        }

        @InternalAPI
        fun updatePriceCategories(categoryJson: UPPriceCategoryJson): Boolean {
            M4PriceManager().updateCategory(
                categoryNew = Json.decodeFromString(categoryJson.catNew),
                categoryOld = Json.decodeFromString(categoryJson.catOld)
            )
            return true
        }

        @InternalAPI
        fun deletePriceCategory(categoryJson: UPPriceCategoryJson): Boolean {
            M4PriceManager().deleteCategory(
                category = Json.decodeFromString(categoryJson.catNew),
            )
            return true
        }
    }
}
