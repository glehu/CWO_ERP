package modules.m3.gui

import tornadofx.Fragment
import tornadofx.action
import tornadofx.button
import tornadofx.form

class MG3InvoicePayer : Fragment("Pay Invoice") {
    var userConfirmed = false
    override val root = form {
        button("Pay (CTRL+S)") {
            shortcut("CTRL+S")
            action {
                userConfirmed = true
                close()
            }
        }
    }
}
