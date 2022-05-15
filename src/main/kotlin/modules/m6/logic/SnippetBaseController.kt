package modules.m6.logic

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m6.Snippet
import modules.mx.logic.Log
import modules.mx.snippetBaseIndexManager

@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
@ExperimentalSerializationApi
@InternalAPI
class SnippetBaseController : IModule {
  override val moduleNameLong = "SnippetBaseController"
  override val module = "M6"
  override fun getIndexManager(): IIndexManager {
    return snippetBaseIndexManager!!
  }

  companion object {
    val mutex = Mutex()
  }

  suspend fun createResource(): Snippet {
    lateinit var snippet: Snippet
    mutex.withLock {
      snippet = Snippet()
      save(snippet)
      log(Log.LogType.SYS, "Resource created")
    }
    return snippet
  }

  suspend fun getResource(gUID: String): Snippet? {
    var snippet: Snippet?
    mutex.withLock {
      snippet = null
      getEntriesFromIndexSearch(gUID, 2, true) {
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
}
