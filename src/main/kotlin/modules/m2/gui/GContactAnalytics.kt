package modules.m2.gui

import io.ktor.util.*
import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.chart.PieChart
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.logic.ContactAnalytics
import modules.mx.gui.GProgressbar
import modules.mx.m2GlobalIndex
import tornadofx.*

@InternalAPI
@ExperimentalSerializationApi
class GContactAnalytics : Fragment("Analytics") {
    private val m2controller: ContactAnalytics by inject()
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
            button("City distribution") {
                prefWidth = 200.0
                action {
                    maxEntries = m2GlobalIndex!!.getLastUniqueID().toInt()
                    runAsync {
                        val cityDist = m2controller.getChartDataOnCityDistribution(m2GlobalIndex!!, numberOfCities)
                        {
                            progressN = it.first
                            updateProgress(it.first.toDouble(), maxEntries.toDouble())
                            updateMessage("${it.second} (${it.first} / $maxEntries)")
                        }
                        ui { showPiechart(cityDist) }
                    }
                }
            }
            add<GProgressbar>()
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
