package modules.mx

import javafx.scene.text.FontWeight
import tornadofx.*

class MXProgressbar : View()
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