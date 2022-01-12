package modules.mx.gui

import db.CwODB
import interfaces.IIndexManager
import io.ktor.util.*
import javafx.collections.ObservableList
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.logic.DiscographyIndexManager
import modules.m2.logic.ContactIndexManager
import modules.m3.logic.InvoiceIndexManager
import modules.m4.logic.ItemIndexManager
import modules.m4.logic.ItemStockPostingIndexManager
import modules.mx.*
import modules.mx.gui.userAlerts.GAlert
import modules.mx.logic.Log
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class GDatabaseManager : View("Databases") {
  /**
   * This list needs to be updated in case a new index manager was added.
   */
  private var indexManagers: ObservableList<IIndexManager> = observableListOf()

  /**
   * This function needs to be updated in case a new index manager was added.
   */
  private fun updateDatabases() {
    indexManagers.clear()
    discographyIndexManager = DiscographyIndexManager()
    contactIndexManager = ContactIndexManager()
    invoiceIndexManager = InvoiceIndexManager()
    itemIndexManager = ItemIndexManager()
    itemStockPostingIndexManager = ItemStockPostingIndexManager()
    indexManagers.addAll(
      discographyIndexManager,
      contactIndexManager,
      invoiceIndexManager,
      itemIndexManager,
      itemStockPostingIndexManager
    )
  }

  init {
    if (discographyIndexManager != null) indexManagers.add(discographyIndexManager)
    if (contactIndexManager != null) indexManagers.add(contactIndexManager)
    if (invoiceIndexManager != null) indexManagers.add(invoiceIndexManager)
    if (itemIndexManager != null) indexManagers.add(itemIndexManager)
    if (itemStockPostingIndexManager != null) indexManagers.add(itemStockPostingIndexManager)
  }

  val table = tableview(indexManagers) {
    readonlyColumn("Database", IIndexManager::module).prefWidth(80.0)
    readonlyColumn("Description", IIndexManager::moduleNameLong).prefWidth(200.0)
    readonlyColumn("# Entries", IIndexManager::lastUID).prefWidth(125.0)
    readonlyColumn("DB Size (MiB)", IIndexManager::dbSizeMiByte).prefWidth(125.0)
    readonlyColumn("Index Size (MiB)", IIndexManager::ixSizeMiByte).prefWidth(125.0)
    readonlyColumn("Last Change", IIndexManager::lastChangeDateLocal).prefWidth(175.0)
    readonlyColumn("by User", IIndexManager::lastChangeUser).prefWidth(175.0)
    columnResizePolicy = SmartResize.POLICY
  }

  override val root = borderpane {
    center = form {
      vbox {
        button("Update Databases") {
          action {
            val prompt = GAlert(
              "This action will reload all indices from the disk. Continue?", true
            )
            prompt.openModal(block = true)
            if (prompt.confirmed.value) {
              updateDatabases()
              GAlert("Successfully reloaded all indices.").openModal()
            }
          }
          prefWidth = rightButtonsWidth
        }
        for (indexManager in indexManagers) {
          val ixModule = indexManager.module.uppercase()
          button("Reset $ixModule") {
            action {
              val prompt = GAlert(
                "This action will fully reset the module $ixModule.",
                true
              )
              prompt.openModal(block = true)
              if (prompt.confirmed.value) {
                CwODB.resetModuleDatabase(ixModule)
                indexManager.setLastChangeData(-1, activeUser.username)
                updateDatabases()
                indexManager.log(
                  logType = Log.LogType.INFO,
                  text = "Database $ixModule reset",
                  moduleAlt = ixModule
                )
                indexManager.log(
                  logType = Log.LogType.INFO,
                  text = "Database $ixModule reset",
                  moduleAlt = "MX"
                )
                GAlert("Successfully reset $ixModule.").openModal()
              }
            }
            prefWidth = rightButtonsWidth
            style { unsafe("-fx-base", Color.DARKRED) }
          }
        }
      }
    }
    right = vbox {
      button("Show log") {
        action {
          GLog("MX").showLog(Log.getLogFile("MX"), "DATABASE".toRegex())
        }
        prefWidth = rightButtonsWidth
      }
    }
  }

  fun refreshStats() {
    table.refresh()
  }
}
