package modules.mx.gui.userAlerts

import modules.mx.logic.MXLog
import tornadofx.Fragment
import tornadofx.form
import tornadofx.label

class MGXUserAlert(type: MXLog.LogType, message: String) : Fragment("User Alert") {
    override val root = form {
        label(message)
    }
}