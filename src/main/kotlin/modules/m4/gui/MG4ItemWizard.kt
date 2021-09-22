package modules.m4.gui

import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.misc.M4ItemModel
import tornadofx.*

@ExperimentalSerializationApi
class M4ItemConfiguratorWizard : Wizard("Add new item") {
    val item: M4ItemModel by inject()

    init {
        enableStepLinks = true
        showHeader = false
        add(NewM4ItemMainData::class)
    }
}

@ExperimentalSerializationApi
class NewM4ItemMainData : Fragment("Main") {
    private val item: M4ItemModel by inject()

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    override val root = form {
        fieldset {
            field("UID") {
                label(item.uID)
            }
            field("Name") { textfield(item.description).required() }
        }
    }

    override fun onSave() {
        isComplete = item.commit()
    }
}