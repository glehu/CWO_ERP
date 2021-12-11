package modules.mx.logic

import modules.mx.getModulePath
import java.io.File

class API {
    companion object {
        enum class AuthType {
            BASIC, TOKEN, NONE
        }

        fun getAPITokenFile(api: String): File {
            val tokenFilePath = File("${getModulePath("MX")}\\api\\$api")
            if (!tokenFilePath.isDirectory) tokenFilePath.mkdirs()
            val tokenFile = File("$tokenFilePath\\${api}_token.json")
            if (!tokenFile.isFile) tokenFile.createNewFile()
            return tokenFile
        }
    }
}
