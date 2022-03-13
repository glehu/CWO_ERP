package modules.m3.gui

import interfaces.IIndexManager
import interfaces.IModule
import io.ktor.util.*
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.scene.control.TabPane
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import modules.m3.logic.InvoiceCLIController
import modules.m3.misc.InvoiceIni
import modules.mx.Statistic
import modules.mx.invoiceIndexManager
import modules.mx.rightButtonsWidth
import tornadofx.Fragment
import tornadofx.action
import tornadofx.borderpane
import tornadofx.button
import tornadofx.checkbox
import tornadofx.column
import tornadofx.combobox
import tornadofx.enableCellEditing
import tornadofx.field
import tornadofx.fieldset
import tornadofx.form
import tornadofx.hbox
import tornadofx.makeEditable
import tornadofx.observableListOf
import tornadofx.readonlyColumn
import tornadofx.tab
import tornadofx.tableview
import tornadofx.tabpane
import tornadofx.textfield
import tornadofx.tooltip
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.mutableMapOf
import kotlin.collections.set

@InternalAPI
@ExperimentalSerializationApi
class GInvoiceSettings : IModule, Fragment("Invoice Settings") {
  override val moduleNameLong = "InvoiceSettings"
  override val module = "M3"
  override fun getIndexManager(): IIndexManager {
    return invoiceIndexManager!!
  }

  private val iniVal = InvoiceCLIController().getIni()

  private var statusTexts = observableListOf<Statistic>()
  private val todoStatuses = SimpleStringProperty(iniVal.todoStatuses)
  private val autoCommission = SimpleBooleanProperty(iniVal.autoCommission)
  private val autoCreateContacts = SimpleBooleanProperty(iniVal.autoCreateContacts)
  private val autoSendEMailConfirmation = SimpleBooleanProperty(iniVal.autoSendEmailConfirmation)
  private var autoStorageSelection = SimpleBooleanProperty(iniVal.autoStorageSelection)
  private var autoStorageSelectionOrder = SimpleStringProperty(iniVal.autoStorageSelectionOrder)
  private var autoStorageSelectionOrderTypes = FXCollections.observableArrayList(
    "LIFO", "FIFO" //, "HIFO", "LOFO", "FEFO"     TODO: not implemented
  )

  override val root = borderpane {
    prefWidth = 800.0
    prefHeight = 500.0
    for ((n, s) in iniVal.statusTexts) {
      val element = Statistic(
        description = "Mapping",
        sValue = s,
        nValue = n.toFloat(),
        number = false
      )
      statusTexts.add(element)
    }
    center = tabpane {
      tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
      tab("Status Settings") {
        form {
          fieldset("Status Texts") {
            tableview(statusTexts) {
              isEditable = true
              readonlyColumn("Status", Statistic::nValue)
              column("Text", Statistic::sValue) {
                makeEditable()
              }
              enableCellEditing()
              isFocusTraversable = false
            }
          }
          fieldset("To Do Quicksearch") {
            field("Statuses") {
              textfield(todoStatuses) {
                tooltip("Displays the To Do relevant statuses, seperated by a comma <,>.")
              }
            }
          }
        }
      }
      tab("Storage Posting") {
        form {
          fieldset("Automatic Storage Selection") {
            field("Auto Mode") { checkbox("", autoStorageSelection) }
            field("Storage Selection Order") { combobox(autoStorageSelectionOrder, autoStorageSelectionOrderTypes) }
          }
        }
      }
      tab("API Settings") {
        form {
          fieldset("Automatic Processing") {
            field("Auto-Commission Web Orders") {
              checkbox(property = autoCommission) {
                tooltip("When checked, puts web orders into status 1.")
              }
            }
            field("Auto-Create new Contacts") {
              checkbox(property = autoCreateContacts)
            }
            field("Auto-Send EMail Confirmation") {
              checkbox(property = autoSendEMailConfirmation)
            }
          }
        }
      }
      tab("Database & Indices") {
        form {
          fieldset("Indices") {
            button("Rebuild indices") {
              //TODO: Not yet implemented
              isDisable = true
              tooltip("Rebuilds all indices in case of faulty indices.")
              prefWidth = rightButtonsWidth
            }
          }
        }
      }
    }
    bottom = hbox {
      button("Save (CTRL+S)") {
        prefWidth = rightButtonsWidth
        shortcut("CTRL+S")
      }.action {
        val newMap = mutableMapOf<Int, String>()
        for (statistic in statusTexts) {
          newMap[statistic.nValue.toInt()] = statistic.sValue
        }
        getSettingsFile().writeText(
          Json.encodeToString(
            InvoiceIni(
              statusTexts = newMap,
              todoStatuses = todoStatuses.value,
              autoCommission = autoCommission.value,
              autoCreateContacts = autoCreateContacts.value,
              autoSendEmailConfirmation = autoSendEMailConfirmation.value,
              autoStorageSelection = autoStorageSelection.value,
              autoStorageSelectionOrder = autoStorageSelectionOrder.value
            )
          )
        )
        close()
      }
    }
  }
}
