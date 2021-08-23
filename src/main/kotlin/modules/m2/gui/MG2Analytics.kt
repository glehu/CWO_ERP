package modules.m2.gui

import db.CwODB
import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.chart.PieChart
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.logic.M2Analytics
import modules.mx.gui.MGXProgressbar
import modules.mx.m2GlobalIndex
import tornadofx.*

@ExperimentalSerializationApi
class MG2Analytics : Fragment("City distribution") {
    val db: CwODB by inject()
    private val m2controller: M2Analytics by inject()
    private val progressProperty = SimpleIntegerProperty()
    private var progressN by progressProperty
    private var maxEntries = 0
    private val numberOfCitiesProperty = SimpleIntegerProperty()
    private var numberOfCities by numberOfCitiesProperty
    override val root = form {
        setPrefSize(800.0, 600.0)
        //Default value
        numberOfCities = 1
        vbox {
            fieldset {
                field("Amount of cities") {
                    textfield(numberOfCitiesProperty) {
                        prefWidth = 50.0
                        maxWidth = 50.0
                        tooltip("Sets the amount of cities to show.")
                    }
                }
            }
            button("Start") {
                action {
                    maxEntries = m2controller.db.getLastUniqueID("M2")
                    runAsync {
                        val cityDist = m2controller.getChartDataOnCityDistribution(m2GlobalIndex, numberOfCities)
                        {
                            progressN = it.first
                            updateProgress(it.first.toDouble(), maxEntries.toDouble())
                            updateMessage("${it.second} (${it.first} / $maxEntries)")
                        }
                        ui { showPiechart(cityDist) }
                    }
                }
            }
            add<MGXProgressbar>()
        }
    }

    private fun showPiechart(cityDist: MutableMap<String, Double>) {
        piechart("City distribution for ${cityDist["[amount]"]!!.toInt()} contacts") {
            data.clear()
            for ((k, v) in cityDist) {
                if (k != "[amount]") {
                    data.add(PieChart.Data("$k (${v.toInt()})", v))
                }
            }
        }
    }
}