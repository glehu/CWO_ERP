package modules.m2.logic

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import db.CwODB
import interfaces.IModule
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.Contact
import modules.m2.misc.ContactModel
import modules.mx.activeUser
import modules.mx.logic.MXLog
import modules.mx.m2GlobalIndex
import tornadofx.Controller
import java.io.File
import kotlin.system.measureTimeMillis

@ExperimentalSerializationApi
class M2Import : IModule, Controller() {
    override fun moduleNameLong() = "M2Import"
    override fun module() = "M2"

    val db: CwODB by inject()

    fun importData(
        file: File,
        contactSchema: ContactModel,
        birthdayHeaderName: String,
        updateProgress: (Pair<Int, String>) -> Unit
    ) {
        MXLog.log(module(), MXLog.LogType.INFO, "Data import start", moduleNameLong())
        val raf = db.openRandomFileAccess(module(), CwODB.RafMode.READWRITE)
        val dbManager = M2DBManager()
        var counter = 0
        val timeInMillis = measureTimeMillis {
            csvReader {
                delimiter = ';'
                charset = "UTF-8"
            }.open(file) {
                readAllWithHeaderAsSequence().forEach { row: Map<String, String> ->
                    counter++
                    val contact = Contact(-1, import(row[contactSchema.name.value].toString(), "NoName"))
                    contact.firstName = import(row[contactSchema.firstName.value].toString())
                    contact.lastName = import(row[contactSchema.lastName.value].toString())
                    contact.street = import(row[contactSchema.street.value].toString())
                    contact.city = import(row[contactSchema.city.value].toString())
                    contact.postCode = import(row[contactSchema.postCode.value].toString())
                    contact.birthdate = import(row[birthdayHeaderName].toString(), "01.01.1980")
                    contact.country = import(row[contactSchema.country.value].toString())

                    dbManager.saveEntry(
                        entry = contact, db,
                        posDB = -1L,
                        byteSize = -1,
                        raf = raf,
                        indexManager = m2GlobalIndex,
                        indexWriteToDisk = false,
                        userName = activeUser.username
                    )
                    updateProgress(Pair(counter, "Importing data..."))
                    if (counter % 5000 == 0) {
                        MXLog.log(module(), MXLog.LogType.INFO, "Data Insertion uID ${contact.uID}", moduleNameLong())
                        runBlocking { launch { m2GlobalIndex.writeIndexData() } }
                    }
                }
            }
            MXLog.log(
                module(), MXLog.LogType.INFO,
                "Data Insertion uID ${db.getLastUniqueID(module())}", moduleNameLong()
            )
            runBlocking { launch { m2GlobalIndex.writeIndexData() } }
        }
        db.closeRandomFileAccess(raf)
        MXLog.log(
            module(),
            MXLog.LogType.INFO,
            "Data Import end (${timeInMillis / 1000} sec)",
            moduleNameLong()
        )
    }

    private fun import(from: String, default: String = "?"): String {
        return if (from != "null") from else default
    }
}