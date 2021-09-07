package modules.m3.gui

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m3.misc.M3ItemModel
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class ItemConfiguratorWizard : Wizard("Add new item") {
    val item: M3ItemModel by inject()

    init {
        enableStepLinks = true
        add(NewItemMainData::class)
    }
}

@InternalAPI
@ExperimentalSerializationApi
class NewItemMainData : Fragment("Main") {
    private val item: M3ItemModel by inject()

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    override val root = form {
        fieldset {
            field("UID") {
                textfield(item.uID).isEditable = false
            }
            field("Description") { textfield(item.description).required() }
            field("Price") {
                hbox {
                    textfield(item.price) {
                        prefWidth = 100.0
                    }.required()
                    label("EUR") { paddingHorizontal = 20 }
                }
            }
            field("Amount") { textfield(item.amount) }
        }
    }

    override fun onSave() {
        isComplete = item.commit()
    }
}