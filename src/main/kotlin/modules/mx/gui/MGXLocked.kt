package modules.mx.gui

import tornadofx.Fragment
import tornadofx.form
import tornadofx.label

class MGXLocked : Fragment("LOCKED") {
    override val root = form {
        label("The entry you are trying to open is currently locked.")
    }
}