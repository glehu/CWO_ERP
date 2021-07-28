package modules.mx.gui

import javafx.scene.text.FontWeight
import tornadofx.*

class MGXProgressbar : View()
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