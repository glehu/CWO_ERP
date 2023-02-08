package modules.m1.logic

import db.CwODB
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.Song
import modules.m1.misc.getGenreList
import modules.mx.discographyIndexManager
import modules.mx.logic.Log
import kotlin.random.Random
import kotlin.system.measureTimeMillis

@InternalAPI
@ExperimentalSerializationApi
class DiscographyBenchmark : IModule {
  override val moduleNameLong = "DiscographyBenchmark"
  override val module = "M1"
  override fun getIndexManager(): IIndexManager {
    return discographyIndexManager!!
  }

  suspend fun insertRandomEntries(amount: Int) {
    log(Log.Type.INFO, "Benchmark entry insertion start")
    val raf = CwODB.openRandomFileAccess(module, CwODB.CwODB.RafMode.READWRITE)
    val timeInMillis = measureTimeMillis {
      for (i in 1..amount) {
        val song = Song(-1, getRandomGenre())
        //Fill it with data
        song.vocalist = song.name.drop(2)
        song.producer = song.name.dropLast(2)
        song.genre = getRandomGenre()
        song.releaseDate = "01.01.1000"
        save(
                entry = song,
                raf = raf,
                indexWriteToDisk = false,
        )
        if (i % 10_000 == 0) {
          log(Log.Type.INFO, "BENCHMARK_INSERTION uID ${song.uID}")
          discographyIndexManager!!.writeIndexData()
        }
      }
    }
    CwODB.closeRandomFileAccess(raf)
    log(Log.Type.INFO, "Benchmark entry insertion end (${timeInMillis / 1000} sec)")
  }

  private fun getRandomGenre(): String {
    val genres = getGenreList()
    return genres[Random.nextInt(0, genres.size)]
  }
}
