package modules.mx.logic

import modules.mx.getModulePath
import java.io.File

class MXAPI
{
    fun getAPITokenFile(api: String): File
    {
        val tokenFilePath = File("${getModulePath("MX")}\\$api")
        if (!tokenFilePath.isDirectory) tokenFilePath.mkdirs()
        val tokenFile = File("$tokenFilePath\\$api")
        if (!tokenFile.isFile) tokenFile.createNewFile()
        return tokenFile
    }
}