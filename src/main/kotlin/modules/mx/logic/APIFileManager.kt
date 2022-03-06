package modules.mx.logic

import modules.mx.getModulePath
import java.io.File
import java.nio.file.Paths

class APIFileManager {
  companion object {
    enum class AuthType {
      BASIC, TOKEN, NONE
    }

    fun getAPITokenFile(api: String): File {
      val path = Paths.get(getModulePath("MX"), "api", api).toString()
      val tokenFilePath = File(path)
      if (!tokenFilePath.isDirectory) tokenFilePath.mkdirs()
      val tokenFile = File(Paths.get(path, "${api}_token.json").toString())
      if (!tokenFile.isFile) tokenFile.createNewFile()
      return tokenFile
    }
  }
}
