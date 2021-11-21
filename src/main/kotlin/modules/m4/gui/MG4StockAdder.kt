package modules.m4.gui

import io.ktor.util.*
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.M4Storage
import modules.m4.misc.M4ItemModel
import modules.m4.misc.getStoragePropertyFromStorage
import tornadofx.*

@ExperimentalSerializationApi
@InternalAPI
class MG4StockAdder : Fragment("Add Stock") {
    private val item: M4ItemModel by inject()
    private var storage = getStoragePropertyFromStorage(M4Storage(0, "default"))
    private var storageNumber = SimpleIntegerProperty(storage.number)
    private var storageDescription = SimpleStringProperty(storage.description)
    private var storageStock = SimpleIntegerProperty(0)
    val stockToAddAmount = SimpleIntegerProperty(0)
    var userConfirmed = false
    override val root = form {
        fieldset("Item Data") {
            field("uID") { label(item.uID) }
            field("Description") { label(item.description) }
        }
        fieldset("Storage Location") {
            field("Number") { label(storageNumber) }
            field("Description") { label(storageDescription) }
            field("Current Stock") { label(storageStock) }
        }
        fieldset("Add Stock") {
            field("Amount") { textfield(stockToAddAmount) }
        }
        button("Add (Enter)") {
            shortcut("Enter")
            action {
                userConfirmed = true
                close()
            }
        }
    }

    fun getStorageData(storage: M4Storage) {
        this.storage = getStoragePropertyFromStorage(storage)
        storageNumber.value = storage.number
        storageDescription.value = storage.description
        //storageStock.value = storage.stock
    }
}
