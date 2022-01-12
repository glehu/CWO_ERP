package interfaces

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi

@InternalAPI
@ExperimentalSerializationApi
interface IController : IModule {
  fun searchEntry()
  suspend fun saveEntry(unlock: Boolean = true)
  fun newEntry()
  fun showEntry(uID: Int)
}
