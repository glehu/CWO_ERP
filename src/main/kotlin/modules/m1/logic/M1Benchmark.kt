package modules.m1.logic

import db.CwODB
import interfaces.IModule
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.Song
import modules.m1.misc.getGenreList
import modules.mx.logic.MXLog
import modules.mx.logic.getRandomString
import modules.mx.m1GlobalIndex
import tornadofx.Controller
import kotlin.random.Random
import kotlin.system.measureTimeMillis

@ExperimentalSerializationApi
class M1Benchmark : IModule, Controller() {
    override fun moduleNameLong() = "M1Benchmark"
    override fun module() = "M1"

    fun insertRandomEntries(amount: Int) {
        MXLog.log(module(), MXLog.LogType.INFO, "Benchmark entry insertion start", moduleNameLong())
        val raf = CwODB.openRandomFileAccess(module(), CwODB.CwODB.RafMode.READWRITE)
        val timeInMillis = measureTimeMillis {
            for (i in 1..amount) {
                val song = Song(-1, getRandomString(10L))
                //Fill it with data
                song.vocalist = getRandomString(10L)
                song.producer = getRandomString(10L)
                song.genre = getRandomGenre()
                song.releaseDate = "01.01.1000"
                save(
                    entry = song,
                    raf = raf,
                    indexManager = m1GlobalIndex,
                    indexWriteToDisk = false,
                )
                if (i % 5000 == 0) {
                    MXLog.log(module(), MXLog.LogType.INFO, "BENCHMARK_INSERTION uID ${song.uID}", moduleNameLong())
                    runBlocking { launch { m1GlobalIndex.writeIndexData() } }
                }
            }
        }
        CwODB.closeRandomFileAccess(raf)
        MXLog.log(
            module(),
            MXLog.LogType.INFO,
            "Benchmark entry insertion end (${timeInMillis / 1000} sec)",
            moduleNameLong()
        )
    }

    private fun getRandomGenre(): String {
        val genres = getGenreList()
        return genres[Random.nextInt(0, genres.size)]
    }
}