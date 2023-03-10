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

  fun getProjectJsonFile(
    project: String,
    filename: String,
    extension: String = "json",
    check: Boolean = true
  ): File {
    if (check) checkProjectJsonFile(project, filename, extension = extension)
    return File(Paths.get(getWebAppPath(project), "$filename.$extension").toString())
  }

  private fun checkProjectJsonFile(
    project: String,
    filename: String,
    extension: String = "json",
    createIfMissing: Boolean = true
  ) {
    if (project.isEmpty()) return
    checkProjectDir(project)
    val projectFile = getProjectJsonFile(project, filename, extension, false)
    if (!projectFile.isFile && createIfMissing) projectFile.createNewFile()
  }

  private fun checkProjectDir(
    project: String,
    createIfMissing: Boolean = true
  ) {
    if (project.isEmpty()) return
    if (!File(getWebAppPath(project)).isDirectory && createIfMissing) File(getWebAppPath(project)).mkdirs()
  }
}
