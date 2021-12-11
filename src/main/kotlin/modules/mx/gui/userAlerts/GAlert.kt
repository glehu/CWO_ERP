package modules.mx.gui.userAlerts

import tornadofx.Fragment
import tornadofx.form
import tornadofx.label

class GAlert(message: String) : Fragment() {
    override val root = form {
        label(message)
    }
}
