package modules.m2.gui

import db.CwODB
import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.chart.PieChart
import javafx.scene.text.FontWeight
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.logic.M2Analytics
import modules.m2.logic.M2IndexManager
import tornadofx.*

@ExperimentalSerializationApi
class MG2Analytics : Fragment("Genre distribution")
{
    val db: CwODB by inject()
    val indexManager: M2IndexManager by inject(Scope(db))
    private val m2controller: M2Analytics by inject()
    private val progressProperty = SimpleIntegerProperty()
    private var progressN by progressProperty
    private var maxEntries = 0
    override val root = form {
        vbox {
            button("Start") {
                action {
                    maxEntries = m2controller.db.getLastUniqueID("M2")
                    runAsync {
                        val genreDist = m2controller.getChartDataOnCityDistribution(indexManager) {
                            progressN = it.first
                            updateProgress(it.first.toDouble(), maxEntries.toDouble())
                            updateMessage("${it.second} (${it.first} / $maxEntries)")
                        }
                        ui { showPiechart(genreDist) }
                    }
                }
            }
            add<ProgressView>()
        }
    }

    private fun showPiechart(genreDist: MutableMap<String, Double>)
    {
        piechart("City distribution for ${genreDist["[amount]"]!!.toInt()} contacts") {
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

    class ProgressView : View() {
        private val status: TaskStatus by inject()

        override val root = vbox(4) {
            visibleWhen { status.running }
            label(status.title).style { fontWeight = FontWeight.BOLD }
            vbox(4) {
                label(status.message)
                progressbar(status.progress)
                visibleWhen { status.running }
            }
        }
    }
}