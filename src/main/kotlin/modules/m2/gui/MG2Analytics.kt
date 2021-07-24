package modules.m2.gui

import db.CwODB
import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.chart.PieChart
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.logic.M2Analytics
import modules.m2.logic.M2IndexManager
import modules.mx.gui.MXGProgressbar
import tornadofx.*

@ExperimentalSerializationApi
class MG2Analytics : Fragment("City distribution")
{
    val db: CwODB by inject()
    val indexManager: M2IndexManager by inject(Scope(db))
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
            hbox {
                button("Start") {
                    action {
                        maxEntries = m2controller.db.getLastUniqueID("M2")
                        runAsync {
                            val cityDist = m2controller.getChartDataOnCityDistribution(indexManager, numberOfCities)
                            {
                                progressN = it.first
                                updateProgress(it.first.toDouble(), maxEntries.toDouble())
                                updateMessage("${it.second} (${it.first} / $maxEntries)")
                            }
                            ui { showPiechart(cityDist) }
                        }
                    }
                }
                textfield(numberOfCitiesProperty) {
                    prefWidth = 50.0
                    tooltip("Sets the amount of cities to show.")
                }
            }
            add<MXGProgressbar>()
        }
    }

    private fun showPiechart(cityDist: MutableMap<String, Double>)
    {
        piechart("City distribution for ${cityDist["[amount]"]!!.toInt()} contacts") {
            data.clear()
            for ((k, v) in cityDist)
            {
                if (k != "[amount]")
                {
                    data.add(PieChart.Data("$k (${v.toInt()})", v))
                }
            }
        }
    }
}