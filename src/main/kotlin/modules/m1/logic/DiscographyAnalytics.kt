package modules.m1.logic

import db.CwODB
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.Song
import modules.mx.discographyIndexManager
import modules.mx.logic.Log
import kotlin.system.measureTimeMillis

@InternalAPI
@ExperimentalSerializationApi
class DiscographyAnalytics : IModule {
  override val moduleNameLong = "DiscographyAnalytics"
  override val module = "M1"
  override fun getIndexManager(): IIndexManager {
    return discographyIndexManager!!
  }

  enum class DistType {
    GENRE, TYPE
  }

  @ExperimentalSerializationApi
  suspend fun getDistributionChartData(
    distType: DistType,
    updateProgress: (Pair<Long, String>) -> Unit
  ): MutableMap<String, Double> {
    var songCount = 0.0
    val map = mutableMapOf<String, Double>()
    var distTypeData: String
    log(Log.Type.INFO, "Distribution analysis start")
    val timeInMS = measureTimeMillis {
      CwODB.getEntriesFromSearchString(
              searchText = "*", ixNr = 0, exactSearch = false, maxSearchResults = -1,
              indexManager = discographyIndexManager!!) { uID, entryBytes ->
        updateProgress(Pair(uID, "Mapping data..."))
        val song: Song? = try {
          decode(entryBytes) as Song
        } catch (e: Exception) {
          println(e.message)
          null
        }
        if (song != null) {
          if (song.uID != -1L) {
            songCount++
            distTypeData = when (distType) {
              DistType.GENRE -> song.genre
              DistType.TYPE -> song.type
            }
            if (map.containsKey(distTypeData)) {
              map[distTypeData] = map[distTypeData]!! + 1.0
            } else {
              map[distTypeData] = 1.0
            }
          }
        }
      }
      map["[amount]"] = songCount
    }
    log(Log.Type.INFO, "Distribution analysis end (${timeInMS / 1000} sec)")
    return map.toSortedMap()
  }
}
