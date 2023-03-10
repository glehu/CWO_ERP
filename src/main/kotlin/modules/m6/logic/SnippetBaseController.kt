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
import org.imgscalr.Scalr
import java.io.ByteArrayInputStream
import java.io.FileOutputStream
import java.util.*
import javax.imageio.ImageIO

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

  suspend fun getSnippet(guid: String): Snippet? {
    var snippet: Snippet?
    mutex.withLock {
      snippet = null
      getEntriesFromIndexSearch(guid, 1, true) {
        snippet = it as Snippet
      }
    }
    return snippet
  }

  /**
   * Saves a Base64 [String] to a file on the disk.
   *
   * Accepts:
   * + Images: JPEG, PNG
   * + Audio: MP3, WAV
   *
   * In case an Image is provided, there is the option to pass two optional values:
   *
   * + maxWidth: [Int]
   * + maxHeight: [Int] (maxWidth if no valid value provided)
   *
   * If maxWidth (with or without maxHeight) is provided, the image will be resized (Imgscalr)
   * accordingly, respecting the images original aspect ratio.
   *
   * Also updates the provided [Snippet], saving all necessary info like link and type.
   * @return the updated [Snippet]
   *
   */
  suspend fun saveFile(
    base64: String,
    snippet: Snippet,
    owner: String,
    maxWidth: Int? = null,
    maxHeight: Int? = null
  ): Snippet? {
    // Get Bytes from Base64 String
    val strings: List<String> = base64.split(",")
    val mimeType = strings[0]
    val decoder = Base64.getDecoder()
    val decodedBytes: ByteArray = decoder.decode(strings[1])
    var image = withContext(Dispatchers.IO) {
      ImageIO.read(ByteArrayInputStream(decodedBytes))
    }
    // Get the file extension
    var fileExtension: String? = null
    if (mimeType.contains("image")) {
      // Image types
      if (mimeType.contains("jpeg")) {
        fileExtension = "jpg"
      } else if (mimeType.contains("png")) {
        fileExtension = "png"
      } else if (mimeType.contains("gif")) {
        fileExtension = "gif"
      }
    } else if (mimeType.contains("audio")) {
      // Audio types
      if (mimeType.contains("mpeg")) {
        fileExtension = "mp3"
      } else if (mimeType.contains("wav")) {
        fileExtension = "wav"
      }
    }
    // Exit upon reaching this point without having found a supported media type
    if (fileExtension == null) return null
    val file = getProjectJsonFile(owner, snippet.guid, extension = fileExtension)
    // Create the resource and save it to the disk
    if (mimeType.contains("image") && !mimeType.contains("gif")) {
      // Resizing necessary?
      if (maxWidth != null && maxWidth > 0) {
        val maxTrueHeight = if (maxHeight == null || maxHeight < 1) {
          maxWidth
        } else maxHeight
        image = Scalr.resize(
                image, Scalr.Method.AUTOMATIC, Scalr.Mode.AUTOMATIC, maxWidth, maxTrueHeight, Scalr.OP_ANTIALIAS)
      }
      withContext(Dispatchers.IO) {
        ImageIO.write(image, fileExtension, file)
        image.flush() // Flush to free system resources
      }
    } else {
      // Write Bytes to File
      withContext(Dispatchers.IO) {
        val fos = FileOutputStream(file)
        fos.write(decodedBytes)
        fos.flush()
        fos.close()
      }
    }
    //Update Snippet
    snippet.payloadType = "url:file"
    snippet.payload = file.absolutePath
    // Save and return
    saveResource(snippet)
    return snippet
  }

  private suspend fun saveResource(snippet: Snippet) {
    mutex.withLock {
      save(snippet)
    }
  }
}
