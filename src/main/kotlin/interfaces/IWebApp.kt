package interfaces

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.getModulePath
import java.nio.file.Paths

@ExperimentalSerializationApi
@InternalAPI
interface IWebApp: IModule {
  val webAppName: String
  fun getWebAppPath(path: String): String {
    return getModulePath(Paths.get("webapp",webAppName.lowercase(),path).toString())
  }
}
