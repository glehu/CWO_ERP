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

  fun getProjectJsonFile(project: String, check: Boolean = true): File {
    if (check) checkProjectJsonFile(project)
    return File(Paths.get(getWebAppPath(project), "$project.json").toString())
  }

  private fun checkProjectJsonFile(project: String, createIfMissing: Boolean = true) {
    if (project.isEmpty()) return
    checkProjectDir(project)
    val projectFile = getProjectJsonFile(project, false)
    if (!projectFile.isFile && createIfMissing) projectFile.createNewFile()
  }

  private fun checkProjectDir(project: String, createIfMissing: Boolean = true) {
    if (project.isEmpty()) return
    if (!File(getWebAppPath(project)).isDirectory && createIfMissing) File(getWebAppPath(project)).mkdirs()
  }
}
