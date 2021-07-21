package modules.m2.logic

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import db.CwODB
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.IModule
import modules.m2.Contact
import modules.m2.misc.ContactModel
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

    fun importData(
        file: File,
        contactSchema: ContactModel,
        birthdayHeaderName: String,
        updateProgress: (Pair<Int, String>) -> Unit
    )
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
                    val contact = Contact(-1, import(row[contactSchema.name.value].toString(), "NoName"))
                    contact.firstName = import(row[contactSchema.firstName.value].toString())
                    contact.lastName = import(row[contactSchema.lastName.value].toString())
                    contact.street = import(row[contactSchema.street.value].toString())
                    contact.city = import(row[contactSchema.city.value].toString())
                    contact.postCode = import(row[contactSchema.postCode.value].toString())
                    contact.birthdate = import(row[birthdayHeaderName].toString(), "01.01.1980")
                    contact.country = import(row[contactSchema.country.value].toString())

                    dbManager.saveEntry(contact, db, -1L, -1, raf, indexManager, false)
                    updateProgress(Pair(counter, "Importing data..."))
                    if (counter % 5000 == 0)
                    {
                        MXLog.log("M2", MXLog.LogType.INFO, "Data Insertion uID ${contact.uID}", moduleName())
                        runBlocking { launch { indexManager.writeIndexData() } }
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

    private fun import(from: String, default: String = "?"): String
    {
        return if (from != "null") from else default
    }
}