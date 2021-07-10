package modules.m2.gui

import db.CwODB
import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.text.FontWeight
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m2.logic.M2Import
import modules.m2.logic.M2IndexManager
import tornadofx.*
import java.io.File

@ExperimentalSerializationApi
class MG2Import : Fragment("Genre distribution")
{
    val db: CwODB by inject()
    val indexManager: M2IndexManager by inject(Scope(db))
    private val m2controller: M2Import by inject()
    private val progressProperty = SimpleIntegerProperty()
    private var progressN by progressProperty
    private var maxEntries = 0
    override val root = form {
        vbox {
            button("Start") {
                action {
                    maxEntries = 110_000
                    runAsync {
                        m2controller.importData(File(
                            "C:\\Users\\duffy\\Documents\\TopM\\gbdaten.csv")) {
                            progressN = it.first
                            updateProgress(it.first.toDouble(), maxEntries.toDouble())
                            updateMessage("${it.second} (${it.first} / $maxEntries)")
                        }
                    }
                }
            }
            add<ProgressView>()
        }
    }

    class ProgressView : View()
    {
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