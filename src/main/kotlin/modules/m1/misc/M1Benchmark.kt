package modules.m1.misc

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import modules.IModule
import modules.m1.Song
import modules.m1.getGenreList
import modules.m1.logic.M1DBManager
import modules.m1.logic.M1IndexManager
import modules.mx.logic.MXLog
import tornadofx.Controller
import tornadofx.Scope
import kotlin.random.Random
import kotlin.streams.asSequence
import kotlin.system.measureTimeMillis

@ExperimentalSerializationApi
class M1Benchmark : IModule, Controller()
{
    override fun moduleName() = "M1Benchmark"

    val db: CwODB by inject()
    val indexManager: M1IndexManager by inject(Scope(db))

    fun insertRandomEntries(amount: Int)
    {
        MXLog.log("M1", MXLog.LogType.INFO, "Benchmark entry insertion start ${MXLog.timestamp()}", moduleName())
        val raf = db.openRandomFileAccess("M1", "rw")
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

                dbManager.saveEntry(song, db, -1L, -1, raf, indexManager, false)

                if (i % 5000 == 0)
                {
                    MXLog.log("M1", MXLog.LogType.INFO, "BENCHMARK_INSERTION uID ${song.uID}", moduleName())
                    indexManager.writeIndexData()
                }
            }
        }
        db.closeRandomFileAccess(raf)
        MXLog.log(
            "M1",
            MXLog.LogType.INFO,
            "Benchmark entry insertion end ${MXLog.timestamp()} (${timeInMillis / 1000} sec)",
            moduleName()
        )
    }

    private fun getRandomGenre(): String
    {
        val genres = getGenreList()
        return genres[Random.nextInt(0, genres.size)]
    }

    private fun getRandomString(size: Long): String
    {
        val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return java.util.Random().ints(size, 0, letters.length)
            .asSequence()
            .map(letters::get)
            .joinToString("")
    }
}