package modules.m6.logic

import api.logic.core.ServerController
import api.misc.json.SnippetLink
import api.misc.json.SnippetsOfUniChatroomPayload
import interfaces.IIndexManager
import interfaces.IModule
import interfaces.IWebApp
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m5.UniChatroom
import modules.m5.logic.UniChatroomController
import modules.m6.Snippet
import modules.m7knowledge.Knowledge
import modules.m7knowledge.logic.KnowledgeController
import modules.m7wisdom.Wisdom
import modules.m7wisdom.logic.WisdomController
import modules.mx.logic.Log
import modules.mx.logic.Timestamp
import modules.mx.logic.UserCLIManager
import modules.mx.snippetBaseIndexManager
import org.imgscalr.Scalr
import java.io.ByteArrayInputStream
import java.io.FileOutputStream
import java.math.RoundingMode
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.ceil

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

  suspend fun saveResource(snippet: Snippet) {
    mutex.withLock {
      save(snippet)
    }
  }

  /**
   * Saves a Base64 [String] to a file on the disk.
   *
   * Accepts:
   *
   * + Images: .JPEG, .PNG, .GIF
   * + Audio: .MP3, .WAV
   * + Text: .TXT, .CSV, .MD, .XML, .CSS
   * + Application: .ZIP, .7Z, .RAR, .GZ, .PDF, .JSON, .XML, .DOC, .DOCX, .XLS, .XLSX
   *
   * In case an Image is provided, there is the option to pass two optional values:
   *
   * + maxWidth:  [Int]
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
    filename: String,
    base64: String,
    snippet: Snippet,
    owner: String,
    maxWidth: Int? = null,
    maxHeight: Int? = null,
    fileSize: Double = 0.0,
    srcUniChatroomUID: Long? = -1L,
    srcWisdomUID: Long? = -1L,
    srcProcessUID: Long? = -1L
  ): Snippet? {
    // Get Bytes from Base64 String
    val strings: List<String> = base64.split(",")
    val mimeType = strings[0]
    val decoder = Base64.getDecoder()
    val decodedBytes: ByteArray = decoder.decode(strings[1])
    // Get the file extension
    val fileExtension: String = getFileExtensionToMimeType(mimeType) ?: return null
    // Get the file size
    val sizeInMB: Double = if (fileSize > 0.0) {
      fileSize
    } else {
      // Trigger Warning: Maths
      /* We calculate the file size with the following formula:
          Math.Ceiling(base64 / 4) * 3 * 0.000001
        Explanation:
          Base64 String is 133,33% bigger (usually) than the true file size
            -> we need to divide by 4 and multiply by 3
          We want to compare the MB size for convenience
            -> we multiply by 0.000001 to convert B to MB
       */
      ceil((base64.length / 4).toDouble()) * 3 * 0.000001
    }
    val nameOfFile: String
    if (filename.isNotEmpty()) {
      nameOfFile = filename.take(100).replace('.', '-').replace(' ', '-') + '-' + snippet.guid
      snippet.payloadName = filename
      if (!snippet.payloadName.endsWith(fileExtension)) {
        snippet.payloadName += ".$fileExtension"
      }
    } else {
      nameOfFile = snippet.guid
      snippet.payloadName = snippet.guid + '.' + fileExtension
    }
    val file = getProjectJsonFile(owner, nameOfFile, extension = fileExtension)
    // Create the resource and save it to the disk
    if (mimeType.contains("image") && !mimeType.contains("gif")) {
      var image = withContext(Dispatchers.IO) {
        ImageIO.read(ByteArrayInputStream(decodedBytes))
      }
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
    snippet.payloadSizeMB = sizeInMB.toBigDecimal().setScale(2, RoundingMode.UP).toDouble()
    if (srcUniChatroomUID != null && srcUniChatroomUID != 1L) snippet.srcUniChatroomUID = srcUniChatroomUID
    if (srcWisdomUID != null && srcWisdomUID != 1L) snippet.srcWisdomUID = srcWisdomUID
    if (srcProcessUID != null && srcProcessUID != 1L) snippet.srcProcessUID = srcProcessUID
    snippet.payloadMimeType = mimeType
    // Save and return
    saveResource(snippet)
    return snippet
  }

  private fun getFileExtensionToMimeType(mimeType: String): String? {
    var fileExtension: String? = null
    if (mimeType.contains("image/")) {
      // Image types
      if (mimeType.contains("jpeg")) {
        fileExtension = "jpg"
      } else if (mimeType.contains("png")) {
        fileExtension = "png"
      } else if (mimeType.contains("gif")) {
        fileExtension = "gif"
      }
    } else if (mimeType.contains("audio/")) {
      // Audio types
      if (mimeType.contains("mpeg")) {
        fileExtension = "mp3"
      } else if (mimeType.contains("wav")) {
        fileExtension = "wav"
      }
    } else if (mimeType.contains("text/")) {
      // Text types
      if (mimeType.contains("plain")) {
        fileExtension = "txt"
      } else if (mimeType.contains("csv")) {
        fileExtension = "csv"
      } else if (mimeType.contains("markdown")) {
        fileExtension = "md"
      } else if (mimeType.contains("xml")) {
        fileExtension = "xml"
      } else if (mimeType.contains("css")) {
        fileExtension = "css"
      }
    } else if (mimeType.contains("application/")) {
      // Application types
      if (mimeType.contains("zip")) {
        fileExtension = "zip"
      } else if (mimeType.contains("7z")) {
        fileExtension = "7z"
      } else if (mimeType.contains("vnd.rar")) {
        fileExtension = "rar"
      } else if (mimeType.contains("gzip")) {
        fileExtension = "gz"
      } else if (mimeType.contains("/pdf")) {
        fileExtension = "pdf"
      } else if (mimeType.contains("/json")) {
        fileExtension = "json"
      } else if (mimeType.contains("/xml")) {
        fileExtension = "xml"
      } else if (mimeType.contains("vnd.ms-excel")) {
        fileExtension = "xls"
      } else if (mimeType.contains("vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
        fileExtension = "xlsx"
      } else if (mimeType.contains("msword")) {
        fileExtension = "doc"
      } else if (mimeType.contains("vnd.openxmlformats-officedocument.wordprocessingml.document")) {
        fileExtension = "docx"
      }
    }
    return fileExtension
  }

  suspend fun httpGetSnippetsOfUniChatroom(
    appCall: ApplicationCall,
    srcUniChatroomGUID: String
  ) {
    val username = UserCLIManager.getUserFromEmail(ServerController.getJWTEmail(appCall))!!.username
    val uniChatroom: UniChatroom?
    // Retrieve chatroom and check for rights
    with(UniChatroomController()) {
      uniChatroom = getChatroom(srcUniChatroomGUID)
      if (uniChatroom == null) {
        appCall.respond(HttpStatusCode.NotFound)
        return
      }
      if (!uniChatroom.checkIsMember(username) || uniChatroom.checkIsMemberBanned(username)) {
        appCall.respond(HttpStatusCode.Forbidden)
        return
      }
    }
    // Retrieve snippets and generate download links
    val snippetsResponse = SnippetsOfUniChatroomPayload()
    getEntriesFromIndexSearch(uniChatroom!!.uID.toString(), 2, true) {
      it as Snippet
      snippetsResponse.snippets.add(
              SnippetLink(
                      url = "https://wikiric.xyz/m6/get/${it.guid}", filename = it.payloadName,
                      filesizeMB = it.payloadSizeMB, filetype = it.payloadMimeType,
                      filedate = Timestamp.getUTCTimestampFromHex(it.dateCreated), guid = it.guid))
    }
    appCall.respond(snippetsResponse)
  }

  suspend fun httpGetSnippetsOfWisdom(
    appCall: ApplicationCall,
    wisdomGUID: String
  ) {
    val wisdom: Wisdom?
    // Retrieve chatroom and check for rights
    with(WisdomController()) {
      wisdom = getWisdomFromGUID(wisdomGUID)
      if (wisdom == null) {
        appCall.respond(HttpStatusCode.NotFound)
        return
      }
      val knowledgeController = KnowledgeController()
      val knowledge = knowledgeController.get(wisdom.knowledgeUID) as Knowledge
      if (!knowledgeController.httpCanAccessKnowledge(appCall, knowledge)) return
    }
    // Retrieve snippets and generate download links
    val snippetsResponse = SnippetsOfUniChatroomPayload()
    getEntriesFromIndexSearch(wisdom!!.uID.toString(), 3, true) {
      it as Snippet
      snippetsResponse.snippets.add(
              SnippetLink(
                      url = "https://wikiric.xyz/m6/get/${it.guid}", filename = it.payloadName,
                      filesizeMB = it.payloadSizeMB, filetype = it.payloadMimeType,
                      filedate = Timestamp.getUTCTimestampFromHex(it.dateCreated), guid = it.guid))
    }
    appCall.respond(snippetsResponse)
  }

  suspend fun httpGetSnippetsOfProcess(
    appCall: ApplicationCall,
    processGUID: String
  ) {
    val process: Wisdom?
    // Retrieve chatroom and check for rights
    with(WisdomController()) {
      process = getWisdomFromGUID(processGUID)
      if (process == null) {
        appCall.respond(HttpStatusCode.NotFound)
        return
      }
      val knowledgeController = KnowledgeController()
      val knowledge = knowledgeController.get(process.knowledgeUID) as Knowledge
      if (!knowledgeController.httpCanAccessKnowledge(appCall, knowledge)) return
    }
    // Retrieve snippets and generate download links
    val snippetsResponse = SnippetsOfUniChatroomPayload()
    getEntriesFromIndexSearch(process!!.uID.toString(), 4, true) {
      it as Snippet
      snippetsResponse.snippets.add(
              SnippetLink(
                      url = "https://wikiric.xyz/m6/get/${it.guid}", filename = it.payloadName,
                      filesizeMB = it.payloadSizeMB, filetype = it.payloadMimeType,
                      filedate = Timestamp.getUTCTimestampFromHex(it.dateCreated), guid = it.guid))
    }
    appCall.respond(snippetsResponse)
  }
}
