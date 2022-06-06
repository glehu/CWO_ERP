package modules.m6.logic

import interfaces.IIndexManager
import interfaces.IModule
import interfaces.IWebApp
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m6.Snippet
import modules.mx.logic.Log
import modules.mx.snippetBaseIndexManager
import sun.misc.BASE64Decoder
import java.io.ByteArrayInputStream
import java.io.FileOutputStream

@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
class SnippetBaseController : IModule, IWebApp {
  override val webAppName = "SnippetBase"
  override val moduleNameLong = "SnippetBaseController"
  override val module = "M6"
  override fun getIndexManager(): IIndexManager {
    return snippetBaseIndexManager!!
  }

  companion object {
    val mutex = Mutex()
  }

  suspend fun createSnippet(): Snippet {
    lateinit var snippet: Snippet
    mutex.withLock {
      snippet = Snippet()
      save(snippet)
      log(Log.Type.SYS, "Resource created")
    }
    return snippet
  }

  suspend fun getSnippet(gUID: String): Snippet? {
    var snippet: Snippet?
    mutex.withLock {
      snippet = null
      getEntriesFromIndexSearch(gUID, 1, true) {
        snippet = it as Snippet
      }
    }
    return snippet
  }

  /**
   * Saves a Base64 encoded Image to an Image File on the Server.
   * Updates the provided snippet, saving all necessary info like link and type.
   */
  suspend fun saveImage(base64: String, type: String, snippet: Snippet): Snippet? {
    //Get Bytes from Base64 String
    val strings: List<String> = base64.split(",")
    val decoder = BASE64Decoder()
    val decodedBytes: ByteArray = decoder.decodeBuffer(strings[1])
    val bis = ByteArrayInputStream(decodedBytes)
    withContext(Dispatchers.IO) {
      bis.close()
    }
    //Write Bytes to File
    var imgType: String? = null
    if (type.contains("jpeg")) {
      imgType = "jpg"
    } else if (type.contains("png")) {
      imgType = "png"
    }
    if (imgType == null) return null
    val file = getProjectJsonFile(snippet.gUID, extension = imgType)
    withContext(Dispatchers.IO) {
      val fos = FileOutputStream(file)
      fos.write(decodedBytes)
      fos.flush()
      fos.close()
    }
    //Update Snippet
    snippet.payloadType = "url:file"
    snippet.payload = file.absolutePath
    return snippet
  }

  suspend fun saveResource(snippet: Snippet) {
    mutex.withLock {
      save(snippet)
    }
  }
}
