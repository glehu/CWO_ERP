package modules.m1Discography.gui

import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.chart.PieChart
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1Discography.logic.M1Analytics
import modules.mx.gui.MGXProgressbar
import modules.mx.m1GlobalIndex
import tornadofx.*

@ExperimentalSerializationApi
class MG1Analytics : Fragment("Analytics") {
    private val m1controller: M1Analytics by inject()
    private val progressProperty = SimpleIntegerProperty()
    private var progressN by progressProperty
    private var maxEntries = 0
    override val root = form {
        setPrefSize(800.0, 600.0)
        vbox {
            button("Genre Distribution") {
                prefWidth = 200.0
                action {
                    maxEntries = m1GlobalIndex!!.getLastUniqueID().toInt()
                    runAsync {
                        val genreDist =
                            m1controller.getDistributionChartData(M1Analytics.DistType.GENRE) {
                                progressN = it.first
                                updateProgress(it.first.toDouble(), maxEntries.toDouble())
                                updateMessage("${it.second} (${it.first} / $maxEntries)")
                            }
                        ui { showPiechart(genreDist) }
                    }
                }
            }
            button("Type Distribution") {
                prefWidth = 200.0
                action {
                    maxEntries = m1GlobalIndex!!.getLastUniqueID().toInt()
                    runAsync {
                        val genreDist =
                            m1controller.getDistributionChartData(M1Analytics.DistType.TYPE) {
                                progressN = it.first
                                updateProgress(it.first.toDouble(), maxEntries.toDouble())
                                updateMessage("${it.second} (${it.first} / $maxEntries)")
                            }
                        ui { showPiechart(genreDist) }
                    }
                }
            }
            add<MGXProgressbar>()
        }
    }

    private fun showPiechart(genreDist: MutableMap<String, Double>) {
        piechart("Distribution for ${genreDist["[amount]"]!!.toInt()} entries") {
            data.clear()
            for ((k, v) in genreDist) {
                if (k != "[amount]") {
                    data.add(PieChart.Data("$k (${v.toInt()})", v))
                }
            }
        }
    }
}
