package modules.mx.gui.userAlerts

import tornadofx.Fragment
import tornadofx.form
import tornadofx.label

class MGXUserAlert(message: String) : Fragment() {
    override val root = form {
        label(message)
    }
}