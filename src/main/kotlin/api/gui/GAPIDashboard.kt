package api.gui

import api.logic.APIDashboard
import io.ktor.util.*
import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.gui.GProgressbar
import modules.mx.usageTracker
import tornadofx.View
import tornadofx.action
import tornadofx.areachart
import tornadofx.button
import tornadofx.form
import tornadofx.getValue
import tornadofx.hbox
import tornadofx.observableListOf
import tornadofx.setValue
import tornadofx.vbox

@InternalAPI
@ExperimentalSerializationApi
class GAPIDashboard : View("API Dashboard") {
  private val progressProperty = SimpleIntegerProperty()
  private var progressN by progressProperty
  private var maxEntries: Long = 0L
  private lateinit var webModuleUsageWeek: MutableMap<String, MutableMap<String, Int>>
  private lateinit var webModuleUsageDay: MutableMap<String, MutableMap<String, Int>>
  private var chartWeek = areachart("Web Module Tracking (8 Days)", CategoryAxis(), NumberAxis())
  private var chartDay = areachart("Web Module Tracking (Today)", CategoryAxis(), NumberAxis())
  override val root = form {
    vbox {
      button("Get Data") {
        prefWidth = 200.0
        action {
          getWebModuleUsageData()
        }
      }
      hbox {
        add(chartWeek)
        add(chartDay)
      }
      add<GProgressbar>()
    }
  }

  fun getWebModuleUsageData() {
    if (usageTracker == null) return
    maxEntries = usageTracker!!.totalAPICalls.get()
    runAsync {
      webModuleUsageWeek =
        APIDashboard().getWebUsageDays(8) {
          progressN = it.first
          updateProgress(it.first.toDouble(), maxEntries.toDouble())
          updateMessage("${it.second} (${it.first} / $maxEntries)")
        }
      webModuleUsageDay =
        APIDashboard().getWebUsageToday {
          progressN = it.first
          updateProgress(it.first.toDouble(), maxEntries.toDouble())
          updateMessage("${it.second} (${it.first} / $maxEntries)")
        }
      ui {
        chartWeek.data.clear()
        for (data in webModuleUsageWeek) {
          val dataList = observableListOf<XYChart.Data<String, Number>>()
          for (tstamp in data.value) {
            dataList.add(XYChart.Data(tstamp.key, tstamp.value))
          }
          chartWeek.data.add(XYChart.Series(data.key, dataList))
        }
        chartDay.data.clear()
        for (data in webModuleUsageDay) {
          val dataList = observableListOf<XYChart.Data<String, Number>>()
          for (tstamp in data.value) {
            dataList.add(XYChart.Data(tstamp.key, tstamp.value))
          }
          chartDay.data.add(XYChart.Series(data.key, dataList))
        }
      }
    }
  }
}
