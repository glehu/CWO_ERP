package modules.m1.logic

import db.CwODB
import interfaces.IIndexManager
import interfaces.IModule
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.M1Song
import modules.m1.misc.getGenreList
import modules.mx.logic.MXLog
import modules.mx.logic.getRandomString
import modules.mx.m1GlobalIndex
import tornadofx.Controller
import kotlin.random.Random
import kotlin.system.measureTimeMillis

@ExperimentalSerializationApi
class M1Benchmark : IModule, Controller() {
    override val moduleNameLong = "M1Benchmark"
    override val module = "M1"
    override fun getIndexManager(): IIndexManager {
        return m1GlobalIndex
    }

    suspend fun insertRandomEntries(amount: Int) {
        log(MXLog.LogType.INFO, "Benchmark entry insertion start")
        val raf = CwODB.openRandomFileAccess(module, CwODB.CwODB.RafMode.READWRITE)
        val timeInMillis = measureTimeMillis {
            for (i in 1..amount) {
                val song = M1Song(-1, getRandomString(10L))
                //Fill it with data
                song.vocalist = getRandomString(10L)
                song.producer = getRandomString(10L)
                song.genre = getRandomGenre()
                song.releaseDate = "01.01.1000"
                save(
                    entry = song,
                    raf = raf,
                    indexWriteToDisk = false,
                )
                if (i % 5000 == 0) {
                    log(MXLog.LogType.INFO, "BENCHMARK_INSERTION uID ${song.uID}")
                    coroutineScope { launch { m1GlobalIndex.writeIndexData() } }
                }
            }
        }
        CwODB.closeRandomFileAccess(raf)
        log(MXLog.LogType.INFO, "Benchmark entry insertion end (${timeInMillis / 1000} sec)")
    }

    private fun getRandomGenre(): String {
        val genres = getGenreList()
        return genres[Random.nextInt(0, genres.size)]
    }
}