package modules.m2.gui

import db.CwODB
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.text.FontWeight
import javafx.stage.FileChooser
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
    private val filenameProperty = SimpleStringProperty()
    private lateinit var file: File
    private var progressN by progressProperty
    private val buttonWidth = 150.0
    override val root = form {
        vbox {
            hbox {
                button("Choose file") {
                    action {
                        file = chooseFile(
                            "Choose file",
                            arrayOf(FileChooser.ExtensionFilter("CSV file (*.csv)", "*.csv")),
                            FileChooserMode.Single
                        )[0]
                        if (file.isFile) filenameProperty.value = file.name
                    }
                    prefWidth = buttonWidth
                }
                label(filenameProperty) {
                    paddingHorizontal = 20
                }
            }
            button("Start") {
                action {
                    runAsync {
                        m2controller.importData(file) {
                            progressN = it.first
                            updateProgress(it.first.toDouble(), it.first.toDouble())
                            updateMessage("${it.second} ${it.first}")
                        }
                    }
                }
                prefWidth = buttonWidth
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