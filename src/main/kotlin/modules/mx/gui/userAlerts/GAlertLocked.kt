package modules.mx.gui.userAlerts

import tornadofx.Fragment
import tornadofx.form
import tornadofx.label

class GAlertLocked : Fragment("LOCKED") {
    override val root = form {
        label("The entry you are trying to open is currently locked.")
    }
}
