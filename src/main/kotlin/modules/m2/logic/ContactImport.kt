package modules.m2.logic

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import db.CwODB
import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.Contact
import modules.m2.misc.ContactModel
import modules.mx.contactIndexManager
import modules.mx.logic.Log
import tornadofx.Controller
import java.io.File
import kotlin.system.measureTimeMillis

@InternalAPI
@ExperimentalSerializationApi
class ContactImport : IModule, Controller() {
  override val moduleNameLong = "ContactImport"
  override val module = "M2"
  override fun getIndexManager(): IIndexManager {
    return contactIndexManager!!
  }

  suspend fun importData(
    file: File,
    contactSchema: ContactModel,
    birthdayHeaderName: String,
    updateProgress: (Pair<Int, String>) -> Unit
  ) {
    log(Log.Type.INFO, "Data import start")
    val raf = CwODB.openRandomFileAccess(module, CwODB.CwODB.RafMode.READWRITE)
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
          runBlocking {
            launch {
              save(
                entry = contact,
                raf = raf,
                indexWriteToDisk = false,
              )
            }
          }
          updateProgress(Pair(counter, "Importing data..."))
          if (counter % 10_000 == 0) {
            log(Log.Type.INFO, "Data Insertion uID ${contact.uID}")
            runBlocking { launch { contactIndexManager!!.writeIndexData() } }
          }
        }
      }
      log(Log.Type.INFO, "Data Insertion uID ${contactIndexManager!!.getLastUniqueID()}")
      coroutineScope { launch { contactIndexManager!!.writeIndexData() } }
    }
    CwODB.closeRandomFileAccess(raf)
    log(Log.Type.INFO, "Data Import end (${timeInMillis / 1000} sec)")
  }

  private fun import(from: String, default: String = "?"): String {
    return if (from != "null") from else default
  }
}
