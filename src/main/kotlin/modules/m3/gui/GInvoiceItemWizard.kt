package modules.m3.gui

import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m3.misc.InvoiceItemModel
import tornadofx.Fragment
import tornadofx.Wizard
import tornadofx.field
import tornadofx.fieldset
import tornadofx.form
import tornadofx.hbox
import tornadofx.label
import tornadofx.paddingHorizontal
import tornadofx.required
import tornadofx.textfield

@InternalAPI
@ExperimentalSerializationApi
class ItemConfiguratorWizard : Wizard("Add new item") {
  val item: InvoiceItemModel by inject()

  init {
    enableStepLinks = true
    add(InvoiceItemMainData::class)
  }
}

@InternalAPI
@ExperimentalSerializationApi
class InvoiceItemMainData : Fragment("Main") {
  private val item: InvoiceItemModel by inject()

  //----------------------------------v
  //----------- Main Data ------------|
  //----------------------------------^
  override val root = form {
    prefWidth = 500.0
    fieldset {
      field("UID") {
        label(item.uID)
      }
      field("Description") {
        textfield(item.description).required()
      }
      field("Price") {
        hbox {
          textfield(item.price) {
            prefWidth = 200.0
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
