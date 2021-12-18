package modules.m4.gui

import io.ktor.util.*
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.ItemStorage
import modules.m4.ItemStorageUnit
import modules.m4.misc.ItemModel
import modules.m4.misc.getStoragePropertyFromStorage
import tornadofx.*

@ExperimentalSerializationApi
@InternalAPI
class GItemStockAdder : Fragment("Add Stock") {
    private val item: ItemModel by inject()
    private var storage = getStoragePropertyFromStorage(ItemStorage(0, ""))
    private var storageNumber = SimpleIntegerProperty(storage.number)
    private var storageDescription = SimpleStringProperty(storage.description)
    var storageUnitNumber = SimpleIntegerProperty(0)
    private var storageUnitDescription = SimpleStringProperty("")
    private var storageUnitStock = SimpleDoubleProperty(0.0)
    val stockToAddAmount = SimpleDoubleProperty(0.0)
    var userConfirmed = false
    override val root = form {
        fieldset("Item Data") {
            field("uID") { label(item.uID) }
            field("Description") { label(item.description) }
        }
        fieldset("Storage") {
            field("Number") { label(storageNumber) }
            field("Description") { label(storageDescription) }
        }
        fieldset("Storage Unit") {
            field("Number") { label(storageUnitNumber) }
            field("Description") { label(storageUnitDescription) }
            field("Current Stock") { label(storageUnitStock) }
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

    fun getStorageData(storage: ItemStorage, storageUnit: ItemStorageUnit) {
        this.storage = getStoragePropertyFromStorage(storage)
        storageNumber.value = storage.number
        storageDescription.value = storage.description
        storageUnitNumber.value = storageUnit.number
        storageUnitDescription.value = storageUnit.description
        storageUnitStock.value = storageUnit.stock
    }

    /**
     * Checks if the StockAdder's data are valid.
     * Currently, only checks if the user confirmed and if the stock to add is not zero.
     * @return true, if the stock can be added.
     */
    fun valid(): Boolean {
        return userConfirmed && stockToAddAmount.value != 0.0
    }
}