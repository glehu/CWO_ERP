package interfaces

import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi
interface IEntry {
  /**
   * The entry's unique identifier.
   */
  var uID: Int

  /**
   * Used to initialize an entry, e.g. setting the unique identifier when creating a new entry.
   */
  fun initialize()
}
