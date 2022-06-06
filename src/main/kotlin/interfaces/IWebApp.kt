package interfaces

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.getModulePath
import java.io.File
import java.nio.file.Paths

@ExperimentalSerializationApi
@InternalAPI
interface IWebApp : IModule {
  val webAppName: String
  fun getWebAppPath(path: String): String {
    return getModulePath(Paths.get("webapp", webAppName.lowercase(), path).toString())
  }

  fun getProjectJsonFile(project: String, check: Boolean = true, extension: String = "json"): File {
    if (check) checkProjectJsonFile(project, extension = extension)
    return File(Paths.get(getWebAppPath(project), "$project.$extension").toString())
  }

  private fun checkProjectJsonFile(project: String, createIfMissing: Boolean = true, extension: String = "json") {
    if (project.isEmpty()) return
    checkProjectDir(project)
    val projectFile = getProjectJsonFile(project, false, extension)
    if (!projectFile.isFile && createIfMissing) projectFile.createNewFile()
  }

  private fun checkProjectDir(project: String, createIfMissing: Boolean = true) {
    if (project.isEmpty()) return
    if (!File(getWebAppPath(project)).isDirectory && createIfMissing) File(getWebAppPath(project)).mkdirs()
  }
}
