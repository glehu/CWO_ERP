package modules.m4.gui

import io.ktor.util.*
import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.M4PriceCategory
import modules.m4.M4Storage
import modules.m4.M4StorageUnit
import modules.m4.Statistic
import modules.m4.logic.M4Controller
import modules.m4.logic.M4StorageManager
import modules.m4.misc.M4ItemModel
import modules.mx.gui.MGXImageViewer
import modules.mx.gui.userAlerts.MGXUserAlert
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class M4ItemConfiguratorWizard : Wizard("Add new item") {
    val item: M4ItemModel by inject()

    init {
        enableStepLinks = true
        showHeader = false
        add(NewM4ItemMainData::class)
        add(NewM4ItemStatistics::class)
        add(NewM4ItemPricesData::class)
        add(NewM4ItemStorageData::class)
    }
}

@InternalAPI
@ExperimentalSerializationApi
class NewM4ItemMainData : Fragment("Main") {
    private val item: M4ItemModel by inject()

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    override val root = form {
        prefWidth = 600.0
        fieldset {
            field("UID") {
                label(item.uID)
            }
            field("Description") { textfield(item.description).required() }
            field("Article Nr") { textfield(item.articleNumber).required() }
            field("EAN Code") { textfield(item.ean).required() }
            field("Manufacturer Nr") { textfield(item.manufacturerNr).required() }
            field("Image Path") {
                textfield(item.imagePath) {
                    isEditable = false
                }
            }
            hbox {
                button("Choose file") {
                    action {
                        item.imagePath.value = chooseFile(
                            "Choose file",
                            arrayOf(FileChooser.ExtensionFilter("PNG file (*.png)", "*.png")),
                            mode = FileChooserMode.Single
                        )[0].path
                    }
                }
                button("View image") {
                    action {
                        if (item.imagePath.value != "?") {
                            MGXImageViewer(Image("file:///${item.imagePath.value}")).openModal()
                        } else {
                            MGXUserAlert("No image loaded.").openModal()
                        }
                    }
                }
            }
        }
    }

    override fun onSave() {
        isComplete = item.commit()
    }
}

@ExperimentalSerializationApi
@InternalAPI
class NewM4ItemStatistics : Fragment("Statistics") {
    private val item: M4ItemModel by inject()
    private var table = tableview(item.statistics) {
        isEditable = true
        column("Description", Statistic::description) {
            prefWidth = 250.0
            makeEditable()
        }
        column("Value", Statistic::sValue) {
            prefWidth = 250.0
            makeEditable()
        }
        enableCellEditing()
        regainFocusAfterEdit()
        isFocusTraversable = false
    }

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    override val root = form {
        fieldset {
            item.uID.addListener { _, _, _ ->
                table.refresh()
            }
            add(table)
            hbox {
                button("Add Statistic") {
                    action {
                        item.statistics.value.add(
                            Statistic("<Description>", "", 0.0F, false)
                        )
                        table.refresh()
                    }
                }
                button("Remove Statistic") {
                    action {
                        item.statistics.value.remove(table.selectedItem)
                    }
                    tooltip("Removes the selected statistic from the item.")
                    style { unsafe("-fx-base", Color.DARKRED) }
                }
            }
        }
    }

    override fun onSave() {
        isComplete = item.commit()
    }
}

@InternalAPI
@ExperimentalSerializationApi
class NewM4ItemPricesData : Fragment("Prices") {
    private val item: M4ItemModel by inject()
    private var table = tableview(item.priceCategories) {
        isEditable = true
        readonlyColumn("Number", M4PriceCategory::number).prefWidth = 100.0
        readonlyColumn("Description", M4PriceCategory::description).prefWidth = 250.0
        column("Price", M4PriceCategory::grossPrice) {
            prefWidth = 100.0
            makeEditable()
        }
        enableCellEditing()
        regainFocusAfterEdit()
        isFocusTraversable = false
    }

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    override val root = form {
        fieldset {
            item.uID.addListener { _, _, _ ->
                table.refresh()
            }
            add(table)
        }
    }

    override fun onSave() {
        isComplete = item.commit()
    }
}

@InternalAPI
@ExperimentalSerializationApi
class NewM4ItemStorageData : Fragment("Stock") {
    private val item: M4ItemModel by inject()
    private val lastItemUID = SimpleIntegerProperty(item.uID.value)
    private val storageManager: M4StorageManager by inject()
    private var selectedStorage: M4Storage = M4Storage(0, "")
    private var storagesTable = tableview(item.storages) {
        isEditable = true
        readonlyColumn("#", M4Storage::number)
        readonlyColumn("Description", M4Storage::description).remainingWidth()
        readonlyColumn("Lock", M4Storage::locked) {
            cellFormat {
                text = ""
                style {
                    backgroundColor = storageManager.getLockedCellColor(it)
                }
            }
        }
        onUserSelect {
            storageUnitsList.clear()
            selectedStorage = it
            for (sUnit in it.storageUnits) {
                storageUnitsList.add(sUnit)
            }
            storageUnitsTable.refresh()
            storageUnitsTable.requestResize()
        }
        enableCellEditing()
        regainFocusAfterEdit()
        columnResizePolicy = SmartResize.POLICY
        isFocusTraversable = false
    }

    var storageUnitsList = observableListOf<M4StorageUnit>()
    private var storageUnitsTable = tableview(storageUnitsList) {
        readonlyColumn("#", M4StorageUnit::number)
        readonlyColumn("Description", M4StorageUnit::description).remainingWidth()
        readonlyColumn("Stock", M4StorageUnit::stock)
        column("Add", M4StorageUnit::locked)
            .cellFormat {
                graphic = hbox {
                    button("+").action {
                        val stockAdder = find<MG4StockAdder>()
                        stockAdder.getStorageData(
                            storage = selectedStorage,
                            storageUnit = rowItem
                        )
                        stockAdder.openModal(block = true)
                        if (stockAdder.userConfirmed && stockAdder.stockToAddAmount.value != 0) {
                            rowItem.stock += stockAdder.stockToAddAmount.value
                            refresh()
                            requestLayout()
                            runBlocking {
                                M4Controller().saveEntry(unlock = false)
                            }
                        }
                    }
                }
            }
        column("Lock", M4StorageUnit::locked) {
            makeEditable()
        }
        columnResizePolicy = SmartResize.POLICY
        isFocusTraversable = false
    }

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    override val root = borderpane {
        item.uID.addListener { _, _, _ ->
            if (item.uID.value == -1 || item.uID.value != lastItemUID.value) {
                storagesTable.refresh()
                storageUnitsList.clear()
                storageUnitsTable.refresh()
                lastItemUID.value = item.uID.value
            }
        }
        left = form {
            add(storagesTable)
            useMaxWidth = true
            minWidth = 500.0
        }
        center = form {
            add(storageUnitsTable)
        }
    }

    override fun onSave() {
        isComplete = item.commit()
    }
}
