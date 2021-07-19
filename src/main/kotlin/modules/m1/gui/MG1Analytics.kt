package modules.m1.gui

import db.CwODB
import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.chart.PieChart
import javafx.scene.text.FontWeight
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.logic.M1Analytics
import modules.m1.logic.M1IndexManager
import modules.mx.MXProgressbar
import tornadofx.*

@ExperimentalSerializationApi
class MG1Analytics : Fragment("Genre distribution")
{
    val db: CwODB by inject()
    val indexManager: M1IndexManager by inject(Scope(db))
    private val m1controller: M1Analytics by inject()
    private val progressProperty = SimpleIntegerProperty()
    private var progressN by progressProperty
    private var maxEntries = 0
    override val root = form {
        vbox {
            button("Start") {
                action {
                    maxEntries = m1controller.db.getLastUniqueID("M1")
                    runAsync {
                        val genreDist = m1controller.getChartDataOnGenreDistribution(indexManager) {
                            progressN = it.first
                            updateProgress(it.first.toDouble(), maxEntries.toDouble())
                            updateMessage("${it.second} (${it.first} / $maxEntries)")
                        }
                        ui { showPiechart(genreDist) }
                    }
                }
            }
            add<MXProgressbar>()
        }
    }

    private fun showPiechart(genreDist: MutableMap<String, Double>)
    {
        piechart("Genre distribution for ${genreDist["[amount]"]!!.toInt()} songs") {
            data.clear()
            for ((k, v) in genreDist)
            {
                if (k != "[amount]")
                {
                    data.add(PieChart.Data("$k (${v.toInt()})", v))
                }
            }
        }
    }
}