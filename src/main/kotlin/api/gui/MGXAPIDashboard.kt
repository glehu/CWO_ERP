package api.gui

import api.logic.MXAPIDashboard
import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.gui.MGXProgressbar
import modules.mx.usageTracker
import tornadofx.*

@ExperimentalSerializationApi
class MGXAPIDashboard : View("API Dashboard") {
    private val progressProperty = SimpleIntegerProperty()
    private var progressN by progressProperty
    private var maxEntries: Long = 0L
    private lateinit var webModuleUsage: MutableMap<String, MutableMap<String, Int>>
    private var chart = linechart("Web Module Tracking (8 Days)", CategoryAxis(), NumberAxis())
    override val root = form {
        vbox {
            button("Get Data") {
                prefWidth = 200.0
                action {
                    getWebModuleUsageData()
                }
            }
            add(chart)
            add<MGXProgressbar>()
        }
    }

    fun getWebModuleUsageData() {
        maxEntries = usageTracker.totalAPICalls.get()
        runAsync {
            webModuleUsage =
                MXAPIDashboard().getWebModuleUsageData {
                    progressN = it.first
                    updateProgress(it.first.toDouble(), maxEntries.toDouble())
                    updateMessage("${it.second} (${it.first} / $maxEntries)")
                }
            ui {
                chart.data.clear()
                for (data in webModuleUsage) {
                    val dataList = observableListOf<XYChart.Data<String, Number>>()
                    for (tstamp in data.value) {
                        dataList.add(XYChart.Data(tstamp.key, tstamp.value))
                    }
                    chart.data.add(XYChart.Series(data.key, dataList))
                }
            }
        }
    }
}
