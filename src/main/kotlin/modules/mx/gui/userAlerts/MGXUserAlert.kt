package modules.mx.gui.userAlerts

import javafx.scene.paint.Color
import javafx.scene.text.Font
import tornadofx.Fragment
import tornadofx.form
import tornadofx.text

class MGXUserAlert(message: String) : Fragment() {
    override val root = form {
        text(message) {
            font = Font(17.0)
            fill = Color.WHITE
        }
    }
}