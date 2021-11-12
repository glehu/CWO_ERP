package modules.m4.gui

import io.ktor.util.*
import javafx.scene.image.Image
import javafx.stage.FileChooser
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.M4PriceCategory
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
        add(NewM4ItemPricesData::class)
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

@InternalAPI
@ExperimentalSerializationApi
class NewM4ItemPricesData : Fragment("Prices") {
    private val item: M4ItemModel by inject()
    private var table = tableview(item.priceCategories) {
        isEditable = true
        readonlyColumn("Number", M4PriceCategory::number).prefWidth = 100.0
        readonlyColumn("User", M4PriceCategory::description).prefWidth = 250.0
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
