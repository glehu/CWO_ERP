package modules.m2.gui

import io.ktor.util.*
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.gui.GDiscographyFinder
import modules.m2.misc.ContactModel
import modules.m3.gui.GInvoiceFinder
import modules.mx.Statistic
import tornadofx.Fragment
import tornadofx.Wizard
import tornadofx.action
import tornadofx.button
import tornadofx.checkbox
import tornadofx.column
import tornadofx.contextmenu
import tornadofx.datepicker
import tornadofx.enableCellEditing
import tornadofx.field
import tornadofx.fieldset
import tornadofx.form
import tornadofx.hbox
import tornadofx.item
import tornadofx.label
import tornadofx.makeEditable
import tornadofx.paddingHorizontal
import tornadofx.regainFocusAfterEdit
import tornadofx.required
import tornadofx.selectedItem
import tornadofx.style
import tornadofx.tableview
import tornadofx.textfield
import tornadofx.tooltip

@ExperimentalSerializationApi
@InternalAPI
class ContactConfiguratorWizard : Wizard("Add new contact") {
  val contact: ContactModel by inject()

  init {
    enableStepLinks = true
    showHeader = false
    add(ContactMainData::class)
    add(ContactFinancialData::class)
    add(ContactProfessionData::class)
    add(ContactMiscData::class)
    add(ContactStatistics::class)
  }
}

@ExperimentalSerializationApi
@InternalAPI
class ContactMainData : Fragment("Main Data") {
  private val contact: ContactModel by inject()

  //----------------------------------v
  //----------- Main Data ------------|
  //----------------------------------^
  override val root = form {
    fieldset {
      field("UID") {
        label(contact.uID)
      }
      field("Name") {
        textfield(contact.name) {
          contextmenu {
            item("Contact's Songs").action {
              val finder = GDiscographyFinder()
              finder.openModal()
              finder.modalSearch(contact.name.value, 2)
            }
          }
          required()
        }
      }
      field("Salutation") { textfield(contact.salutation) }
      field("EMail Address") { textfield(contact.email) }
      field("First Name") { textfield(contact.firstName) }
      field("Last Name") { textfield(contact.lastName) }
      field("Birthdate") { datepicker(contact.birthdate) }
      field("Street") { textfield(contact.street) }
      field("Number") { textfield(contact.houseNr) }
      field("City") { textfield(contact.city) }
      field("Postcode") { textfield(contact.postCode) }
      field("Country") { textfield(contact.country) }
    }
  }

  override fun onSave() {
    isComplete = contact.commit()
  }
}

@ExperimentalSerializationApi
@InternalAPI
class ContactFinancialData : Fragment("Financial Data") {
  private val contact: ContactModel by inject()

  //----------------------------------v
  //-------- Financial Data ---------|
  //----------------------------------^
  override val root = form {
    fieldset {
      field("Price Category") { textfield(contact.priceCategory) }
      field("Contact's Sales") {
        hbox {
          textfield(contact.moneyReceived) {
            contextmenu {
              item("Show invoices (as seller)").action {
                val m3Finder = GInvoiceFinder()
                m3Finder.openModal()
                m3Finder.modalSearch("[${contact.name.value}]", 1)
              }
            }
          }.isEditable = false
          label("EUR") { paddingHorizontal = 20 }
        }
      }
      field("Contact's Expenses") {
        hbox {
          textfield(contact.moneySent) {
            contextmenu {
              item("Show invoices (as buyer)").action {
                val m3Finder = GInvoiceFinder()
                m3Finder.openModal()
                m3Finder.modalSearch("[${contact.name.value}]", 2)
              }
            }
          }.isEditable = false
          label("EUR") { paddingHorizontal = 20 }
        }
      }
    }
  }

  override fun onSave() {
    isComplete = contact.commit()
  }
}

class ContactProfessionData : Fragment("Profession Data") {
  private val contact: ContactModel by inject()

  //----------------------------------v
  //-------- Profession Data ---------|
  //----------------------------------^
  override val root = form {
    fieldset {
      field("Vocalist") { checkbox("", contact.isVocalist) }
      field("Producer") { checkbox("", contact.isProducer) }
      field("Instrumentalist") { checkbox("", contact.isInstrumentalist) }
      field("Manager") { checkbox("", contact.isManager) }
      field("Fan") { checkbox("", contact.isFan) }
    }
  }

  override fun onSave() {
    isComplete = contact.commit()
  }
}

class ContactMiscData : Fragment("Misc Data") {
  private val contact: ContactModel by inject()

  //----------------------------------v
  //----------- Misc Data ------------|
  //----------------------------------^
  override val root = form {
    fieldset {
      field("Spotify ID") { textfield(contact.spotifyID) }
    }
  }

  override fun onSave() {
    isComplete = contact.commit()
  }
}

@ExperimentalSerializationApi
@InternalAPI
class ContactStatistics : Fragment("Statistics") {
  private val contact: ContactModel by inject()
  private var table = tableview(contact.statistics) {
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
      contact.uID.addListener { _, _, _ ->
        table.refresh()
      }
      add(table)
      hbox {
        button("Add Statistic") {
          action {
            contact.statistics.value.add(
              Statistic("<Description>", "", 0.0F, false)
            )
            table.refresh()
          }
        }
        button("Remove Statistic") {
          action {
            contact.statistics.value.remove(table.selectedItem)
          }
          tooltip("Removes the selected statistic from the item.")
          style { unsafe("-fx-base", Color.DARKRED) }
        }
      }
    }
  }

  override fun onSave() {
    isComplete = contact.commit()
  }
}
