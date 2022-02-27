package interfaces

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.getModulePath

@ExperimentalSerializationApi
@InternalAPI
interface IWebApp: IModule {
  val webAppName: String
  fun getWebAppPath(path: String): String {
    return getModulePath("webapp/${webAppName.lowercase()}/$path/")
  }
}
