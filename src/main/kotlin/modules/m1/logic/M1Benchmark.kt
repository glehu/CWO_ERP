package modules.m1.logic

import db.CwODB
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import interfaces.IModule
import modules.m1.Song
import modules.m1.getGenreList
import modules.mx.logic.MXLog
import modules.mx.logic.getRandomString
import modules.mx.m1GlobalIndex
import tornadofx.Controller
import kotlin.random.Random
import kotlin.system.measureTimeMillis

@ExperimentalSerializationApi
class M1Benchmark : IModule, Controller()
{
    override fun moduleNameLong() = "M1Benchmark"
    override fun module() = "M1"

    val db: CwODB by inject()

    fun insertRandomEntries(amount: Int)
    {
        MXLog.log(module(), MXLog.LogType.INFO, "Benchmark entry insertion start", moduleNameLong())
        val raf = db.openRandomFileAccess(module(), "rw")
        val dbManager = M1DBManager()
        val timeInMillis = measureTimeMillis {
            for (i in 1..amount)
            {
                val song = Song(-1, getRandomString(10L))
                //Fill it with data
                song.vocalist = getRandomString(10L)
                song.producer = getRandomString(10L)
                song.genre = getRandomGenre()
                song.releaseDate = "01.01.1000"
                dbManager.saveEntry(song, db, -1L, -1, raf, m1GlobalIndex, false)
                if (i % 5000 == 0)
                {
                    MXLog.log(module(), MXLog.LogType.INFO, "BENCHMARK_INSERTION uID ${song.uID}", moduleNameLong())
                    runBlocking { launch { m1GlobalIndex.writeIndexData() } }
                }
            }
        }
        db.closeRandomFileAccess(raf)
        MXLog.log(
            module(),
            MXLog.LogType.INFO,
            "Benchmark entry insertion end (${timeInMillis / 1000} sec)",
            moduleNameLong()
        )
    }

    private fun getRandomGenre(): String
    {
        val genres = getGenreList()
        return genres[Random.nextInt(0, genres.size)]
    }
}