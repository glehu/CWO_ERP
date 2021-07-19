package modules.m2.logic

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import modules.IModule
import modules.m2.Contact
import modules.mx.logic.MXLog
import tornadofx.Controller
import tornadofx.Scope
import java.io.File
import kotlin.system.measureTimeMillis

@ExperimentalSerializationApi
class M2Import : IModule, Controller()
{
    override fun moduleName() = "M2Import"

    val db: CwODB by inject()
    val indexManager: M2IndexManager by inject(Scope(db))

    fun importData(file: File, updateProgress: (Pair<Int, String>) -> Unit)
    {
        MXLog.log("M2", MXLog.LogType.INFO, "Data Import Start", moduleName())
        val raf = db.openRandomFileAccess("M2", "rw")
        val dbManager = M2DBManager()
        var counter = 0
        val timeInMillis = measureTimeMillis {

            csvReader {
                delimiter = ';'
                charset = "UTF-8"
            }.open(file) {
                readAllWithHeaderAsSequence().forEach { row: Map<String, String> ->
                    counter++
                    //Do something
                    val contact = Contact(-1, row["Schluessel"]!!)
                    contact.firstName = row["Name"]!!
                    contact.lastName  = row["Name1"]!!
                    contact.street    = row["Strasse"]!!
                    contact.city      = row["Ort"]!!
                    contact.postCode  = row["Plz"]!!
                    contact.birthdate = "01.01.2000"
                    contact.country = row.toString()

                    dbManager.saveEntry(contact, db, -1L, -1, raf, indexManager, false)
                    updateProgress(Pair(counter, "Importing data..."))

                    if (counter % 5000 == 0)
                    {
                        MXLog.log("M2", MXLog.LogType.INFO, "Data Insertion uID ${contact.uID}", moduleName())
                        indexManager.writeIndexData()
                    }
                }
            }
        }
        db.closeRandomFileAccess(raf)
        MXLog.log(
            "M2",
            MXLog.LogType.INFO,
            "Data Import end (${timeInMillis / 1000} sec)",
            moduleName()
        )
    }

}